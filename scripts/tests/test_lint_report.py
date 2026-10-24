import importlib.util
from pathlib import Path
import unittest
import subprocess
import sys
import tempfile

spec = importlib.util.spec_from_file_location('lint_report', Path(__file__).parents[1] / 'check-lint-report.py')
report = importlib.util.module_from_spec(spec)
spec.loader.exec_module(report)


class LintReportTest(unittest.TestCase):
    log = '\n'.join(f'> Task :{module}:lintAnalyzeRelease' for module in report.MODULES)
    xml = '<issues format="6" by="lint 7.4.2"/>'

    def test_complete_run_accepts_reviewable_warnings(self):
        xml = '<issues by="lint 7.4.2"><issue id="SetTextI18n" severity="Warning"/></issues>'
        result = report.assess(xml, self.log)
        self.assertTrue(result['passed'])
        self.assertEqual({'Warning': 1}, result['severity'])

    def test_zero_errors_without_dependency_analysis_is_incomplete(self):
        self.assertFalse(report.assess(self.xml, '> Task :app:lintAnalyzeRelease')['passed'])

    def test_custom_registry_and_metadata_loading_failures_are_rejected(self):
        for diagnostic in report.INFRA_FAILURES:
            with self.subTest(diagnostic=diagnostic):
                self.assertFalse(report.assess(self.xml, self.log + '\n' + diagnostic)['passed'])

    def test_errors_and_analysis_failures_are_rejected(self):
        for severity, issue in [('Error', 'NewApi'), ('Fatal', 'UnsafeOptInUsageError'), ('Warning', 'LintError')]:
            self.assertFalse(report.assess(f'<issues by="lint 7.4.2"><issue severity="{severity}" id="{issue}"/></issues>', self.log)['passed'])

    def test_gradle_failure_cannot_be_hidden_by_a_clean_xml(self):
        self.assertFalse(report.assess(self.xml, self.log, gradle_exit_code=1)['passed'])

    def test_other_engine_and_malformed_report_are_not_a_success(self):
        for xml in ['<issues by="lint 4.2.2"/>', '<empty/>', 'not xml']:
            with self.assertRaises((ValueError, report.ET.ParseError)):
                report.assess(xml, self.log)

    def test_missing_report_removes_previous_success_summary(self):
        repo = Path(__file__).resolve().parents[2]
        with tempfile.TemporaryDirectory(dir=repo / '.verification') as directory:
            base = Path(directory)
            summary = base / 'summary.json'
            summary.write_text('{"passed":true}')
            log = base / 'run.log'
            log.write_text(self.log)
            result = subprocess.run([sys.executable, str(repo / 'scripts/check-lint-report.py'),
                '--xml', str(base / 'missing.xml'), '--log', str(log), '--out', str(summary)],
                stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            self.assertEqual(1, result.returncode)
            self.assertFalse(summary.exists())

    def test_sdk_support_warning_is_kept_visible(self):
        result = report.assess(self.xml, self.log + '\nAGP was tested up to compileSdk = 33')
        self.assertTrue(result['passed'])
        self.assertTrue(result['compileSdkBeyondAgpTestedWarning'])


if __name__ == '__main__':
    unittest.main()
