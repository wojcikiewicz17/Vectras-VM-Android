from pathlib import Path

from tools.ci.check_java_contracts import (
    DEFAULT_SIGNATURE_REGEX,
    strip_java_comments,
    validate_signature_count,
)


def test_strip_java_comments_removes_line_and_block_comments():
    data = """
    // public static int torusFlowChecksum(int seed, int steps) { return 0; }
    /* public static int torusFlowChecksum(int seed, int steps) { return 0; } */
    public static int torusFlowChecksum(int seed, int steps) { return 1; }
    """
    cleaned = strip_java_comments(data)
    assert cleaned.count("torusFlowChecksum") == 1


def test_validate_signature_count_ignores_commented_signature(tmp_path: Path):
    java_file = tmp_path / "NativeFastPath.java"
    java_file.write_text(
        """
        class NativeFastPath {
            // public static int torusFlowChecksum(int seed, int steps) { return 0; }
            public static int torusFlowChecksum(int seed, int steps) { return 1; }
        }
        """,
        encoding="utf-8",
    )
    assert validate_signature_count(java_file, DEFAULT_SIGNATURE_REGEX, 1) == 0


def test_validate_signature_count_fails_when_duplicate(tmp_path: Path):
    java_file = tmp_path / "NativeFastPath.java"
    java_file.write_text(
        """
        class NativeFastPath {
            public static int torusFlowChecksum(int seed, int steps) { return 1; }
            public static int torusFlowChecksum(int seed, int steps) { return 2; }
        }
        """,
        encoding="utf-8",
    )
    assert validate_signature_count(java_file, DEFAULT_SIGNATURE_REGEX, 1) == 1
