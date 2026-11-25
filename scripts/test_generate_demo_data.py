import unittest
from decimal import Decimal
from pathlib import Path

import psycopg

import generate_demo_data as demo


class AlignDemoMetricsTest(unittest.TestCase):
    def setUp(self):
        demo.load_dotenv(Path(__file__).resolve().parents[1] / ".env")
        self.connection = psycopg.connect(**demo.connection_options())
        self.cursor = self.connection.cursor()
        for definition in (
            "projet (id integer primary key, code text, budget numeric)",
            "utilisateur (id integer primary key, email text)",
            "freelance (id integer primary key, utilisateur_id integer)",
            "affectation (id integer primary key, freelance_id integer, projet_id integer)",
            "tache (id integer primary key, projet_id integer, affectation_id integer, charge_estimee numeric, avancement integer, statut text)",
            "timesheet (id integer primary key, freelance_id integer, statut text)",
            "ligne_timesheet (id integer primary key, timesheet_id integer, tache_id integer, heures numeric)",
            "pv_reception (id integer primary key, timesheet_id integer, montant numeric)",
        ):
            self.cursor.execute(f"CREATE TEMP TABLE {definition}")
        self.cursor.execute("INSERT INTO projet VALUES (1, 'DEMO-TEST', 1)")
        self.cursor.execute("INSERT INTO utilisateur VALUES (1, 'demo-test@demo.cdg.ma')")
        self.cursor.execute("INSERT INTO freelance VALUES (1, 1)")
        self.cursor.execute("INSERT INTO affectation VALUES (1, 1, 1)")
        self.cursor.execute("INSERT INTO tache VALUES (1, 1, 1, 1, 0, 'A_FAIRE'), (2, 1, 1, 1, 0, 'A_FAIRE')")
        self.cursor.execute("INSERT INTO timesheet VALUES (1, 1, 'VALIDEE'), (2, 1, 'BROUILLON')")
        self.cursor.execute("INSERT INTO ligne_timesheet VALUES (1, 1, 1, 72), (2, 1, 2, 36), (3, 2, 1, 100)")
        self.cursor.execute("INSERT INTO pv_reception VALUES (1, 1, 13500)")

    def tearDown(self):
        self.connection.rollback()
        self.connection.close()

    def test_aligned_hours_progress_and_budget_ignore_drafts(self):
        demo.align_demo_project_metrics(self.cursor, [{"id": 1}])
        self.cursor.execute("SELECT charge_estimee, avancement, statut FROM tache ORDER BY id")
        tasks = self.cursor.fetchall()
        self.assertEqual(tasks[0], (Decimal("12.5"), 72, "EN_COURS"))
        self.assertEqual(tasks[1], (Decimal("6.3"), 71, "EN_COURS"))
        weighted = sum(days * progress for days, progress, _ in tasks) / sum(days for days, _, _ in tasks)
        self.cursor.execute("SELECT budget FROM projet WHERE id = 1")
        budget = self.cursor.fetchone()[0]
        self.assertLess(abs(Decimal("13500") / budget * 100 - weighted), Decimal("0.01"))
        self.assertLess(abs(Decimal("108") / (sum(t[0] for t in tasks) * 8) * 100 - weighted), Decimal("0.2"))

    def test_missing_invoice_is_rejected(self):
        self.cursor.execute("DELETE FROM pv_reception")
        with self.assertRaises(demo.DemoDataError):
            demo.align_demo_project_metrics(self.cursor, [{"id": 1}])

    def test_mixed_project_invoice_is_rejected(self):
        self.cursor.execute("INSERT INTO projet VALUES (2, 'OTHER', 1)")
        self.cursor.execute("INSERT INTO affectation VALUES (2, 1, 2)")
        self.cursor.execute("INSERT INTO tache VALUES (3, 2, 2, 1, 0, 'A_FAIRE')")
        self.cursor.execute("INSERT INTO ligne_timesheet VALUES (4, 1, 3, 1)")
        with self.assertRaises(demo.DemoDataError):
            demo.align_demo_project_metrics(self.cursor, [{"id": 1}])


if __name__ == "__main__":
    unittest.main()
