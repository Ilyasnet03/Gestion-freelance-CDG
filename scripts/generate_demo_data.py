#!/usr/bin/env python
"""Generate a replaceable, twelve-month CDG demo dataset in PostgreSQL."""

from __future__ import annotations

import argparse
import json
import os
import random
import urllib.error
import urllib.request
from datetime import date, datetime, time, timedelta
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import Any

import bcrypt
import psycopg
from faker import Faker
from psycopg import Cursor


DEMO_ADMIN_EMAIL = "demo-admin@demo.cdg.ma"
DEMO_PASSWORD = "password123"
DEMO_EMAIL_PATTERN = "demo-%@demo.cdg.ma"
PROJECT_CODE_PATTERN = "DEMO-%"
EXPECTED_ALERT_TYPES = {
    "DEPASSEMENT_BUDGET",
    "RETARD_PROJET",
    "TIMESHEET_EN_RETARD",
    "ANOMALIE_SAISIE",
    "SURCHARGE_FREELANCE",
}

SPECIALTIES = (
    "Développement full-stack",
    "Ingénierie data",
    "DevOps et cloud",
    "Cybersécurité",
    "UX/UI et accessibilité",
    "Pilotage de projets",
)
TASK_TITLES = (
    "Cadrage fonctionnel et ateliers métier",
    "Conception de l'architecture applicative",
    "Développement des services et API",
    "Réalisation de l'interface utilisateur",
    "Tests d'intégration et correction",
    "Documentation et transfert de compétences",
)
PROJECT_THEMES = (
    "Modernisation du parcours épargne",
    "Portail de services numériques",
    "Pilotage des opérations de prévoyance",
    "Consolidation des données métier",
    "Automatisation des processus internes",
    "Sécurisation des échanges documentaires",
)
MOROCCAN_CITIES = ("Rabat", "Casablanca", "Marrakech", "Tanger", "Agadir", "Fès")
TIMESHEET_STATUSES = ("VALIDEE", "SOUMISE", "BROUILLON", "REJETEE")


class DemoDataError(Exception):
    """An actionable error while preparing or verifying demo data."""


def load_dotenv(path: Path) -> None:
    if not path.is_file():
        return

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        value = value.strip().strip("\"'")
        os.environ.setdefault(key.strip(), value)


def connection_options() -> dict[str, Any]:
    try:
        port = int(os.getenv("DB_PORT", "5432"))
    except ValueError as error:
        raise DemoDataError("DB_PORT doit être un nombre.") from error

    return {
        "host": os.getenv("DB_HOST", "localhost"),
        "port": port,
        "dbname": os.getenv("POSTGRES_DB", "freelances"),
        "user": os.getenv("POSTGRES_USER", "app"),
        "password": os.getenv("POSTGRES_PASSWORD", "app_password_2026"),
        "connect_timeout": 5,
    }


def insert_id(cursor: Cursor) -> int:
    row = cursor.fetchone()
    if row is None:
        raise DemoDataError("PostgreSQL n'a pas retourné l'identifiant créé.")
    return int(row[0])


def demo_data_exists(cursor: Cursor) -> bool:
    cursor.execute(
        """
        SELECT EXISTS (SELECT 1 FROM utilisateur WHERE email LIKE %s)
            OR EXISTS (SELECT 1 FROM projet WHERE code LIKE %s)
        """,
        (DEMO_EMAIL_PATTERN, PROJECT_CODE_PATTERN),
    )
    row = cursor.fetchone()
    return bool(row and row[0])


def has_external_lines_on_demo_projects(cursor: Cursor) -> bool:
    cursor.execute(
        """
        SELECT EXISTS (
            SELECT 1
            FROM ligne_timesheet ligne
            JOIN tache ON tache.id = ligne.tache_id
            JOIN projet ON projet.id = tache.projet_id
            JOIN timesheet ON timesheet.id = ligne.timesheet_id
            JOIN freelance ON freelance.id = timesheet.freelance_id
            JOIN utilisateur ON utilisateur.id = freelance.utilisateur_id
            WHERE projet.code LIKE %s
              AND utilisateur.email NOT LIKE %s
        )
        """,
        (PROJECT_CODE_PATTERN, DEMO_EMAIL_PATTERN),
    )
    row = cursor.fetchone()
    return bool(row and row[0])


def delete_demo_data(cursor: Cursor) -> None:
    demo_timesheets = """
        SELECT timesheet.id
        FROM timesheet
        JOIN freelance ON freelance.id = timesheet.freelance_id
        JOIN utilisateur ON utilisateur.id = freelance.utilisateur_id
        WHERE utilisateur.email LIKE 'demo-%@demo.cdg.ma'
    """
    cursor.execute(
        f"""
        DELETE FROM paiement
        WHERE pv_id IN (
            SELECT id FROM pv_reception WHERE timesheet_id IN ({demo_timesheets})
        )
        """
    )
    cursor.execute(
        f"DELETE FROM pv_reception WHERE timesheet_id IN ({demo_timesheets})"
    )
    cursor.execute(
        f"DELETE FROM ligne_timesheet WHERE timesheet_id IN ({demo_timesheets})"
    )
    cursor.execute(
        f"DELETE FROM timesheet WHERE id IN ({demo_timesheets})"
    )
    cursor.execute(
        "DELETE FROM tache WHERE projet_id IN "
        "(SELECT id FROM projet WHERE code LIKE %s)",
        (PROJECT_CODE_PATTERN,),
    )
    cursor.execute(
        "DELETE FROM affectation WHERE projet_id IN "
        "(SELECT id FROM projet WHERE code LIKE %s)",
        (PROJECT_CODE_PATTERN,),
    )
    cursor.execute("DELETE FROM projet WHERE code LIKE %s", (PROJECT_CODE_PATTERN,))
    cursor.execute(
        "DELETE FROM freelance WHERE utilisateur_id IN "
        "(SELECT id FROM utilisateur WHERE email LIKE %s)",
        (DEMO_EMAIL_PATTERN,),
    )
    cursor.execute("DELETE FROM utilisateur WHERE email LIKE %s", (DEMO_EMAIL_PATTERN,))


def one_year_ago(today: date) -> date:
    try:
        return today.replace(year=today.year - 1)
    except ValueError:
        return today.replace(year=today.year - 1, month=2, day=28)


def monday_of(day: date) -> date:
    return day - timedelta(days=day.weekday())


def workload_period(today: date) -> tuple[date, list[date]]:
    grouped_days: dict[date, list[date]] = {}
    for offset in range(6, -1, -1):
        day = today - timedelta(days=offset)
        if day.weekday() < 5:
            grouped_days.setdefault(monday_of(day), []).append(day)

    eligible = [
        (week, days)
        for week, days in grouped_days.items()
        if len(days) >= 3
    ]
    if not eligible:
        raise DemoDataError(
            "Impossible de préparer une semaine de surcharge dans les sept derniers jours."
        )
    return max(eligible, key=lambda item: item[0])


def bcrypt_password(password: str) -> str:
    encoded = bcrypt.hashpw(
        password.encode("utf-8"),
        bcrypt.gensalt(rounds=10),
    ).decode("ascii")
    if encoded.startswith("$2b$"):
        return "$2a$" + encoded[4:]
    return encoded


def create_user(
    cursor: Cursor,
    first_name: str,
    last_name: str,
    email: str,
    role: str,
    password_hash: str,
) -> int:
    cursor.execute(
        """
        INSERT INTO utilisateur (nom, prenom, email, mot_de_passe, role, actif)
        VALUES (%s, %s, %s, %s, %s, TRUE)
        RETURNING id
        """,
        (last_name, first_name, email, password_hash, role),
    )
    return insert_id(cursor)


def create_demo_users(
    cursor: Cursor,
    fake: Faker,
    password_hash: str,
    rng: random.Random,
) -> tuple[int, list[int], list[int], list[str], list[str], list[str]]:
    admin_id = create_user(
        cursor,
        fake.first_name(),
        fake.last_name(),
        DEMO_ADMIN_EMAIL,
        "ADMIN",
        password_hash,
    )

    manager_ids: list[int] = []
    manager_emails: list[str] = []
    for index in range(1, 4):
        email = f"demo-chef-{index:02d}@demo.cdg.ma"
        manager_ids.append(
            create_user(
                cursor,
                fake.first_name(),
                fake.last_name(),
                email,
                "CHEF_PROJET",
                password_hash,
            )
        )
        manager_emails.append(email)

    freelance_ids: list[int] = []
    freelance_emails: list[str] = []
    for index in range(1, 13):
        email = f"demo-freelance-{index:02d}@demo.cdg.ma"
        user_id = create_user(
            cursor,
            fake.first_name(),
            fake.last_name(),
            email,
            "FREELANCE",
            password_hash,
        )
        tjm = Decimal(str(rng.randint(4500, 7500))).quantize(Decimal("0.01"))
        cursor.execute(
            """
            INSERT INTO freelance (utilisateur_id, tjm, specialite, statut)
            VALUES (%s, %s, %s, 'ACTIF')
            RETURNING id
            """,
            (user_id, tjm, rng.choice(SPECIALTIES)),
        )
        freelance_ids.append(insert_id(cursor))
        freelance_emails.append(email)

    return (
        admin_id,
        manager_ids,
        freelance_ids,
        [DEMO_ADMIN_EMAIL],
        manager_emails,
        freelance_emails,
    )


def create_projects_and_tasks(
    cursor: Cursor,
    fake: Faker,
    manager_ids: list[int],
    freelance_ids: list[int],
    start_date: date,
    today: date,
    rng: random.Random,
) -> tuple[list[dict[str, Any]], list[Decimal]]:
    cursor.execute("SELECT id FROM type_projet ORDER BY id")
    type_rows = cursor.fetchall()
    if not type_rows:
        raise DemoDataError(
            "Aucun type de projet n'existe. Initialisez d'abord la base avec l'application."
        )
    project_type_ids = [int(row[0]) for row in type_rows]

    budgets = (
        Decimal("350000.00"),
        Decimal("650000.00"),
        Decimal("2200000.00"),
        Decimal("2200000.00"),
        Decimal("2500000.00"),
        Decimal("2500000.00"),
    )
    target_progress = (20, 27, 34, 40, 50, 60)
    end_offsets = (30, 45, 60, 90, 150, 240)
    projects: list[dict[str, Any]] = []
    applied_tjms: list[Decimal] = [Decimal("0.00")] * len(freelance_ids)

    for project_index in range(6):
        manager_id = manager_ids[project_index % len(manager_ids)]
        project_end = today + timedelta(days=end_offsets[project_index])
        project_code = f"DEMO-{today.year}-{project_index + 1:03d}"
        project_name = (
            f"{PROJECT_THEMES[project_index]} - {MOROCCAN_CITIES[project_index]}"
        )
        description = fake.paragraph(nb_sentences=2)
        cursor.execute(
            """
            INSERT INTO projet
                (code, nom, description, budget, date_debut, date_fin_prevue,
                 statut, type_projet_id, chef_projet_id)
            VALUES (%s, %s, %s, %s, %s, %s, 'EN_COURS', %s, %s)
            RETURNING id
            """,
            (
                project_code,
                project_name,
                description,
                budgets[project_index],
                start_date,
                project_end,
                rng.choice(project_type_ids),
                manager_id,
            ),
        )
        project_id = insert_id(cursor)
        project_freelances = [project_index * 2, project_index * 2 + 1]
        tasks_by_freelance: dict[int, list[int]] = {}

        for freelance_index in project_freelances:
            freelance_id = freelance_ids[freelance_index]
            cursor.execute(
                "SELECT tjm FROM freelance WHERE id = %s",
                (freelance_id,),
            )
            tjm_row = cursor.fetchone()
            if tjm_row is None:
                raise DemoDataError("Le profil freelance créé est introuvable.")
            tjm = Decimal(str(tjm_row[0])).quantize(Decimal("0.01"))
            applied_tjms[freelance_index] = tjm

            cursor.execute(
                """
                INSERT INTO affectation
                    (projet_id, freelance_id, tjm_applique, date_debut, date_fin)
                VALUES (%s, %s, %s, %s, %s)
                RETURNING id
                """,
                (project_id, freelance_id, tjm, start_date, project_end),
            )
            affectation_id = insert_id(cursor)
            task_ids: list[int] = []
            progress = max(
                0,
                min(100, target_progress[project_index] + rng.randint(-3, 3)),
            )
            task_status = "TERMINEE" if progress == 100 else "EN_COURS" if progress else "A_FAIRE"

            for task_index, title in enumerate(TASK_TITLES):
                cursor.execute(
                    """
                    INSERT INTO tache
                        (projet_id, affectation_id, titre, charge_estimee, avancement,
                         statut, date_debut, date_fin)
                    VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                    RETURNING id
                    """,
                    (
                        project_id,
                        affectation_id,
                        f"{title} - lot {task_index + 1}",
                        Decimal(str(rng.randint(40, 120))).quantize(Decimal("0.1")),
                        progress,
                        task_status,
                        start_date,
                        project_end,
                    ),
                )
                task_ids.append(insert_id(cursor))

            tasks_by_freelance[freelance_index] = task_ids

        projects.append(
            {
                "id": project_id,
                "code": project_code,
                "manager_id": manager_id,
                "freelances": project_freelances,
                "tasks": tasks_by_freelance,
            }
        )

    return projects, applied_tjms


def submission_time(week_start: date, now: datetime) -> datetime:
    scheduled = datetime.combine(week_start + timedelta(days=6), time(16, 0))
    return min(scheduled, now)


def create_timesheets(
    cursor: Cursor,
    freelance_ids: list[int],
    projects: list[dict[str, Any]],
    today: date,
    rng: random.Random,
) -> tuple[dict[tuple[int, date], int], dict[tuple[int, date], str]]:
    first_week = monday_of(one_year_ago(today))
    last_week = monday_of(today)
    now = datetime.now().replace(microsecond=0)
    timesheet_ids: dict[tuple[int, date], int] = {}
    statuses: dict[tuple[int, date], str] = {}

    week_starts: list[date] = []
    week = first_week
    while week <= last_week:
        week_starts.append(week)
        week += timedelta(days=7)

    for freelance_index, freelance_id in enumerate(freelance_ids):
        project = projects[freelance_index // 2]
        manager_id = int(project["manager_id"])
        task_ids = project["tasks"][freelance_index]

        for week_index, week_start in enumerate(week_starts):
            status = TIMESHEET_STATUSES[(week_index + freelance_index) % len(TIMESHEET_STATUSES)]
            submit_at = submission_time(week_start, now)
            decision_at = min(submit_at + timedelta(days=2), now)
            motif = (
                "Préciser les activités réalisées et le temps consacré."
                if status == "REJETEE"
                else None
            )
            date_soumission = submit_at if status in ("SOUMISE", "VALIDEE", "REJETEE") else None
            date_decision = decision_at if status in ("VALIDEE", "REJETEE") else None
            valide_par = manager_id if status in ("VALIDEE", "REJETEE") else None

            cursor.execute(
                """
                INSERT INTO timesheet
                    (freelance_id, semaine_debut, statut, motif_rejet,
                     date_soumission, date_decision, valide_par)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                RETURNING id
                """,
                (
                    freelance_id,
                    week_start,
                    status,
                    motif,
                    date_soumission,
                    date_decision,
                    valide_par,
                ),
            )
            timesheet_id = insert_id(cursor)
            timesheet_ids[(freelance_index, week_start)] = timesheet_id
            statuses[(freelance_index, week_start)] = status

            for day_index in range(5):
                entry_date = week_start + timedelta(days=day_index)
                if entry_date > today:
                    break
                hours = Decimal(str(rng.randint(75, 85))) / Decimal("10")
                task_id = task_ids[(week_index + day_index) % len(task_ids)]
                cursor.execute(
                    """
                    INSERT INTO ligne_timesheet (timesheet_id, tache_id, date_saisie, heures)
                    VALUES (%s, %s, %s, %s)
                    """,
                    (timesheet_id, task_id, entry_date, hours),
                )

    return timesheet_ids, statuses


def replace_sheet_lines(
    cursor: Cursor,
    timesheet_id: int,
    task_ids: list[int],
    days: list[date],
    hours_per_day: Decimal,
) -> None:
    cursor.execute("DELETE FROM ligne_timesheet WHERE timesheet_id = %s", (timesheet_id,))
    for index, entry_date in enumerate(days):
        cursor.execute(
            """
            INSERT INTO ligne_timesheet (timesheet_id, tache_id, date_saisie, heures)
            VALUES (%s, %s, %s, %s)
            """,
            (timesheet_id, task_ids[index % len(task_ids)], entry_date, hours_per_day),
        )


def remove_sheet_pv(cursor: Cursor, timesheet_id: int) -> None:
    cursor.execute(
        "DELETE FROM paiement WHERE pv_id IN "
        "(SELECT id FROM pv_reception WHERE timesheet_id = %s)",
        (timesheet_id,),
    )
    cursor.execute("DELETE FROM pv_reception WHERE timesheet_id = %s", (timesheet_id,))


def prepare_analysis_examples(
    cursor: Cursor,
    projects: list[dict[str, Any]],
    timesheet_ids: dict[tuple[int, date], int],
    statuses: dict[tuple[int, date], str],
    today: date,
    now: datetime,
) -> None:
    workload_week, workload_days = workload_period(today)
    workload_sheet_id = timesheet_ids.get((0, workload_week))
    if workload_sheet_id is None:
        raise DemoDataError("La timesheet de démonstration de surcharge est introuvable.")

    workload_hours = (
        Decimal("52.50") / Decimal(len(workload_days))
    ).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)
    workload_project = projects[0]
    workload_manager = int(workload_project["manager_id"])
    workload_submit = submission_time(workload_week, now)
    remove_sheet_pv(cursor, workload_sheet_id)
    cursor.execute(
        """
        UPDATE timesheet
        SET statut = 'VALIDEE', motif_rejet = NULL, date_soumission = %s,
            date_decision = %s, valide_par = %s
        WHERE id = %s
        """,
        (workload_submit, min(workload_submit + timedelta(days=1), now), workload_manager, workload_sheet_id),
    )
    replace_sheet_lines(
        cursor,
        workload_sheet_id,
        workload_project["tasks"][0],
        workload_days,
        workload_hours,
    )
    statuses[(0, workload_week)] = "VALIDEE"

    overdue_day = today - timedelta(days=5)
    overdue_week = monday_of(overdue_day)
    overdue_sheet_id = timesheet_ids.get((1, overdue_week))
    if overdue_sheet_id is None:
        raise DemoDataError("La timesheet de démonstration en retard est introuvable.")

    overdue_project = projects[0]
    overdue_date = today - timedelta(days=4)
    remove_sheet_pv(cursor, overdue_sheet_id)
    cursor.execute(
        """
        UPDATE timesheet
        SET statut = 'SOUMISE', motif_rejet = NULL, date_soumission = %s,
            date_decision = NULL, valide_par = NULL
        WHERE id = %s
        """,
        (datetime.combine(overdue_day, time(9, 0)), overdue_sheet_id),
    )
    replace_sheet_lines(
        cursor,
        overdue_sheet_id,
        overdue_project["tasks"][1],
        [overdue_date],
        Decimal("8.00"),
    )
    statuses[(1, overdue_week)] = "SOUMISE"


def create_pvs_and_payments(
    cursor: Cursor,
    timesheet_ids: dict[tuple[int, date], int],
    statuses: dict[tuple[int, date], str],
    applied_tjms: list[Decimal],
    today: date,
    rng: random.Random,
) -> tuple[int, int]:
    now = datetime.now().replace(microsecond=0)
    pv_count = 0
    payment_count = 0

    for (freelance_index, week_start), timesheet_id in timesheet_ids.items():
        if statuses[(freelance_index, week_start)] != "VALIDEE":
            continue

        cursor.execute(
            """
            SELECT COALESCE(SUM(heures), 0)
            FROM ligne_timesheet
            WHERE timesheet_id = %s
            """,
            (timesheet_id,),
        )
        total_row = cursor.fetchone()
        total_hours = Decimal(str(total_row[0] if total_row else 0)).quantize(Decimal("0.01"))
        if total_hours <= 0:
            continue

        tjm = applied_tjms[freelance_index]
        amount = (total_hours / Decimal("8") * tjm).quantize(Decimal("0.01"))
        number = (
            f"PV-DEMO-{week_start.year}-{freelance_index + 1:02d}-"
            f"{week_start.timetuple().tm_yday:03d}"
        )
        generation_date = min(
            datetime.combine(week_start + timedelta(days=6), time(10, 0)),
            now,
        )
        pdf_path = f"./uploads/pv/{number}.pdf"
        cursor.execute(
            """
            INSERT INTO pv_reception
                (timesheet_id, numero, total_heures, montant, chemin_pdf, date_generation)
            VALUES (%s, %s, %s, %s, %s, %s)
            RETURNING id
            """,
            (timesheet_id, number, total_hours, amount, pdf_path, generation_date),
        )
        pv_id = insert_id(cursor)
        paid = rng.random() < 0.55
        payment_date = (
            min(today, generation_date.date() + timedelta(days=rng.randint(2, 30)))
            if paid
            else None
        )
        reference = f"DEMO-REG-{pv_count + 1:06d}" if paid else None
        cursor.execute(
            """
            INSERT INTO paiement (pv_id, statut, date_paiement, reference)
            VALUES (%s, %s, %s, %s)
            """,
            (pv_id, "PAYE" if paid else "EN_ATTENTE", payment_date, reference),
        )
        pv_count += 1
        payment_count += 1

    return pv_count, payment_count


def verify_analysis_conditions(cursor: Cursor, today: date) -> None:
    budget_projects = (
        f"DEMO-{today.year}-001",
        f"DEMO-{today.year}-002",
    )
    cursor.execute(
        """
        SELECT projet.code, projet.budget,
               COALESCE(SUM(
                   CASE WHEN timesheet.id IS NOT NULL
                       THEN ROUND(ligne_timesheet.heures / 8, 4)
                            * COALESCE(affectation.tjm_applique, 4500)
                       ELSE 0
                   END
               ), 0)
        FROM projet
        LEFT JOIN tache ON tache.projet_id = projet.id
        LEFT JOIN affectation ON affectation.id = tache.affectation_id
        LEFT JOIN ligne_timesheet ON ligne_timesheet.tache_id = tache.id
        LEFT JOIN timesheet ON timesheet.id = ligne_timesheet.timesheet_id
                            AND timesheet.statut = 'VALIDEE'
        WHERE projet.code IN (%s, %s)
        GROUP BY projet.code, projet.budget
        """,
        budget_projects,
    )
    budget_rates: dict[str, Decimal] = {}
    for code, budget, consumed in cursor.fetchall():
        budget_rates[str(code)] = (
            Decimal(str(consumed)) * Decimal("100") / Decimal(str(budget))
        )
    if any(budget_rates.get(code, Decimal("0")) < Decimal("80") for code in budget_projects):
        raise DemoDataError("Les données ne déclencheraient pas l'alerte de budget.")

    cursor.execute(
        """
        SELECT projet.date_debut, projet.date_fin_prevue,
               tache.charge_estimee, tache.avancement
        FROM projet
        JOIN tache ON tache.projet_id = projet.id
        WHERE projet.code = %s
        """,
        (f"DEMO-{today.year}-001",),
    )
    project_rows = cursor.fetchall()
    if not project_rows:
        raise DemoDataError("Le projet de démonstration pour le retard est introuvable.")
    project_start = project_rows[0][0]
    project_end = project_rows[0][1]
    duration = max(1, (project_end - project_start).days)
    elapsed = max(0, (today - project_start).days)
    if today > project_end:
        elapsed = duration
    expected_progress = min(100.0, elapsed / duration * 100.0)
    total_weight = sum(
        float(row[2]) if row[2] is not None and float(row[2]) > 0 else 1.0
        for row in project_rows
    )
    weighted_progress = sum(
        float(row[3] or 0)
        * (float(row[2]) if row[2] is not None and float(row[2]) > 0 else 1.0)
        for row in project_rows
    )
    actual_progress = int(weighted_progress / total_weight + 0.5)
    if expected_progress - actual_progress < 15:
        raise DemoDataError("Les données ne déclencheraient pas l'alerte de retard projet.")

    overdue_cutoff = today - timedelta(days=3)
    cursor.execute(
        """
        SELECT EXISTS (
            SELECT 1
            FROM timesheet
            JOIN ligne_timesheet ON ligne_timesheet.timesheet_id = timesheet.id
            JOIN tache ON tache.id = ligne_timesheet.tache_id
            JOIN projet ON projet.id = tache.projet_id
            WHERE projet.code LIKE %s
              AND timesheet.statut = 'SOUMISE'
              AND timesheet.date_soumission::date <= %s
        )
        """,
        (PROJECT_CODE_PATTERN, overdue_cutoff),
    )
    if not bool(cursor.fetchone()[0]):
        raise DemoDataError("Les données ne déclencheraient pas l'alerte de timesheet en retard.")

    recent_cutoff = today - timedelta(days=6)
    cursor.execute(
        """
        SELECT EXISTS (
            SELECT 1
            FROM ligne_timesheet
            JOIN timesheet ON timesheet.id = ligne_timesheet.timesheet_id
            JOIN tache ON tache.id = ligne_timesheet.tache_id
            JOIN projet ON projet.id = tache.projet_id
            WHERE projet.code LIKE %s
              AND ligne_timesheet.date_saisie >= %s
            GROUP BY timesheet.freelance_id, ligne_timesheet.date_saisie
            HAVING SUM(ligne_timesheet.heures) > 10
        )
        """,
        (PROJECT_CODE_PATTERN, recent_cutoff),
    )
    if not bool(cursor.fetchone()[0]):
        raise DemoDataError("Les données ne déclencheraient pas l'alerte d'anomalie journalière.")

    cursor.execute(
        """
        SELECT EXISTS (
            SELECT 1
            FROM ligne_timesheet
            JOIN timesheet ON timesheet.id = ligne_timesheet.timesheet_id
            JOIN tache ON tache.id = ligne_timesheet.tache_id
            JOIN projet ON projet.id = tache.projet_id
            WHERE projet.code LIKE %s
              AND ligne_timesheet.date_saisie >= %s
            GROUP BY timesheet.freelance_id, timesheet.semaine_debut
            HAVING SUM(ligne_timesheet.heures) > 48
        )
        """,
        (PROJECT_CODE_PATTERN, recent_cutoff),
    )
    if not bool(cursor.fetchone()[0]):
        raise DemoDataError("Les données ne déclencheraient pas l'alerte de surcharge hebdomadaire.")


def generate_dataset(
    cursor: Cursor,
    seed: int,
    today: date,
) -> dict[str, Any]:
    rng = random.Random(seed)
    fake = Faker("fr_FR")
    fake.seed_instance(seed)
    password_hash = bcrypt_password(DEMO_PASSWORD)

    if demo_data_exists(cursor):
        raise DemoDataError(
            "Un jeu DEMO existe déjà. Utilisez --replace-demo-data pour le remplacer."
        )

    start_date = monday_of(one_year_ago(today))
    (
        admin_id,
        manager_ids,
        freelance_ids,
        admin_emails,
        manager_emails,
        freelance_emails,
    ) = create_demo_users(cursor, fake, password_hash, rng)
    projects, applied_tjms = create_projects_and_tasks(
        cursor,
        fake,
        manager_ids,
        freelance_ids,
        start_date,
        today,
        rng,
    )
    timesheet_ids, statuses = create_timesheets(
        cursor,
        freelance_ids,
        projects,
        today,
        rng,
    )
    now = datetime.now().replace(microsecond=0)
    prepare_analysis_examples(
        cursor,
        projects,
        timesheet_ids,
        statuses,
        today,
        now,
    )
    pv_count, payment_count = create_pvs_and_payments(
        cursor,
        timesheet_ids,
        statuses,
        applied_tjms,
        today,
        rng,
    )
    verify_analysis_conditions(cursor, today)
    cursor.execute(
        "SELECT COUNT(*) FROM ligne_timesheet WHERE timesheet_id IN "
        "(SELECT id FROM timesheet WHERE freelance_id = ANY(%s))",
        (freelance_ids,),
    )
    line_row = cursor.fetchone()
    line_count = int(line_row[0]) if line_row else 0

    return {
        "utilisateurs": 1 + len(manager_ids) + len(freelance_ids),
        "freelances": len(freelance_ids),
        "projets": len(projects),
        "affectations": len(freelance_ids),
        "taches": sum(len(tasks) for project in projects for tasks in project["tasks"].values()),
        "timesheets": len(timesheet_ids),
        "lignes": line_count,
        "pv": pv_count,
        "paiements": payment_count,
        "admin_id": admin_id,
        "admin_emails": admin_emails,
        "manager_emails": manager_emails,
        "freelance_emails": freelance_emails,
    }


def post_json(url: str, payload: dict[str, str], token: str | None = None) -> dict[str, Any]:
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers=headers,
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            result = json.loads(response.read().decode("utf-8"))
            if not isinstance(result, dict):
                raise DemoDataError("Le backend a renvoyé un objet JSON inattendu.")
            return result
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")[:400]
        raise DemoDataError(f"Le backend a répondu HTTP {error.code}: {detail}") from error
    except urllib.error.URLError as error:
        raise DemoDataError(f"Backend inaccessible ({error.reason}).") from error
    except json.JSONDecodeError as error:
        raise DemoDataError("Le backend a renvoyé une réponse JSON invalide.") from error


def run_analysis(api_url: str, options: dict[str, Any]) -> dict[str, Any]:
    base_url = api_url.rstrip("/")
    login = post_json(
        f"{base_url}/api/auth/login",
        {"email": DEMO_ADMIN_EMAIL, "motDePasse": DEMO_PASSWORD},
    )
    token = login.get("token")
    if not isinstance(token, str) or not token:
        raise DemoDataError("La connexion au compte administrateur de démonstration a échoué.")

    result = post_json(f"{base_url}/api/analyses/executer", {}, token)
    with psycopg.connect(**options) as connection:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT DISTINCT alerte.type
                FROM alerte
                JOIN projet ON projet.id = alerte.projet_id
                WHERE projet.code LIKE %s AND alerte.traitee = FALSE
                """,
                (PROJECT_CODE_PATTERN,),
            )
            alert_rows = cursor.fetchall()

    found = {str(row[0]) for row in alert_rows}
    missing = EXPECTED_ALERT_TYPES - found
    if missing:
        raise DemoDataError(
            "Analyse exécutée, mais alertes de démonstration manquantes : "
            + ", ".join(sorted(missing))
        )
    return result


def print_summary(counts: dict[str, Any], dry_run: bool) -> None:
    state = "Simulation (aucune donnée conservée)" if dry_run else "Données enregistrées"
    print(f"\n{state} :")
    for label in (
        "utilisateurs",
        "freelances",
        "projets",
        "affectations",
        "taches",
        "timesheets",
        "lignes",
        "pv",
        "paiements",
    ):
        print(f"  {label}: {counts[label]}")

    print(f"\nMot de passe commun des comptes DEMO : {DEMO_PASSWORD}")
    print("ADMIN : " + ", ".join(counts["admin_emails"]))
    print("CHEF_PROJET : " + ", ".join(counts["manager_emails"]))
    print("FREELANCE : " + ", ".join(counts["freelance_emails"]))


def main() -> int:
    project_root = Path(__file__).resolve().parents[1]
    load_dotenv(project_root / ".env")

    parser = argparse.ArgumentParser(
        description="Génère douze mois de données DEMO réalistes dans PostgreSQL."
    )
    parser.add_argument("--seed", type=int, default=2026, help="Graine Faker déterministe.")
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Exécute les insertions puis annule la transaction.",
    )
    parser.add_argument(
        "--replace-demo-data",
        action="store_true",
        help="Remplace uniquement les enregistrements réservés au générateur DEMO.",
    )
    parser.add_argument(
        "--run-analysis",
        action="store_true",
        help="Lance l'analyse Java et vérifie que les cinq types d'alerte sont présents.",
    )
    parser.add_argument(
        "--api-url",
        default=f"http://localhost:{os.getenv('SPRING_BOOT_PORT', '8080')}",
        help="URL de base du backend Spring Boot.",
    )
    args = parser.parse_args()
    if args.dry_run and args.run_analysis:
        parser.error("--run-analysis n'est pas compatible avec --dry-run.")

    options = connection_options()
    today = date.today()
    try:
        with psycopg.connect(**options) as connection:
            with connection.cursor() as cursor:
                if args.replace_demo_data and demo_data_exists(cursor):
                    if has_external_lines_on_demo_projects(cursor):
                        raise DemoDataError(
                            "Remplacement refusé : des timesheets hors DEMO utilisent "
                            "les tâches DEMO."
                        )
                    delete_demo_data(cursor)
                counts = generate_dataset(cursor, args.seed, today)
            if args.dry_run:
                connection.rollback()
    except psycopg.Error as error:
        raise DemoDataError(
            "Connexion ou opération PostgreSQL impossible. Vérifiez le service db, "
            "DB_HOST/DB_PORT et POSTGRES_DB/POSTGRES_USER/POSTGRES_PASSWORD."
        ) from error

    print_summary(counts, args.dry_run)
    if args.run_analysis:
        try:
            result = run_analysis(args.api_url, options)
        except DemoDataError as error:
            raise DemoDataError(
                f"Les données sont enregistrées, mais l'analyse n'a pas abouti : {error}"
            ) from error
        print("\nCinq types d'alerte vérifiés sur les projets DEMO.")
        print(json.dumps(result, ensure_ascii=False, indent=2))
    elif not args.dry_run:
        print(
            "\nPour créer les alertes, lancez « Exécuter les analyses » dans l'application "
            "ou relancez ce script avec --run-analysis."
        )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except DemoDataError as error:
        raise SystemExit(f"Erreur : {error}") from error
