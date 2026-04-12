import unittest
from pathlib import Path

from tools.verify_repo_file_dependencies import verify_legacy_build_references


class VerifyLegacyBuildReferencesTests(unittest.TestCase):
    def test_detects_occurrence_in_assignment(self) -> None:
        text = 'legacy_path = "android/app/build.gradle"\n'
        findings = verify_legacy_build_references(Path("script.gradle"), text)
        self.assertEqual(findings, ["script.gradle:1"])

    def test_detects_occurrence_in_argument_with_line_continuation(self) -> None:
        text = 'run_tool --input \\\n  android/app/build.gradle\n'
        findings = verify_legacy_build_references(Path("check.sh"), text)
        self.assertEqual(findings, ["check.sh:1"])

    def test_ignores_occurrence_in_comment(self) -> None:
        text = '# android/app/build.gradle\nreal_value="ok"\n'
        findings = verify_legacy_build_references(Path("check.sh"), text)
        self.assertEqual(findings, [])

    def test_ignores_explicit_error_message_literal(self) -> None:
        text = 'fail "Arquivo legado ausente: android/app/build.gradle"\n'
        findings = verify_legacy_build_references(Path("validate.sh"), text)
        self.assertEqual(findings, [])


if __name__ == "__main__":
    unittest.main()
