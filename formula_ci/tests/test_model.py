"""
Unit tests for the example transformation function defined in `formula_ci/model.py`.

These tests verify that the weighted sum implemented in `F` produces the
expected next state for a variety of input conditions. While the weights
themselves are arbitrary, consistent outcomes are essential for CI-based
validation. Running these tests in a CI pipeline ensures that any future
modification to the transformation logic will be detected immediately.
"""

import math
import os
import sys

import pytest

# Add the parent directory of `formula_ci` to sys.path so that
# `formula_ci` can be imported when running tests outside of a package context.
TESTS_DIR = os.path.dirname(os.path.abspath(__file__))
# Insert the parent of the project directory into sys.path so Python can find
# the `formula_ci` package when running tests directly. Without this,
# `import formula_ci` fails because the package resolves relative to the project
# root's parent.
PROJECT_ROOT = os.path.dirname(TESTS_DIR)
PARENT_DIR = os.path.dirname(PROJECT_ROOT)
sys.path.insert(0, PARENT_DIR)

from formula_ci.model import SystemState, F

def test_basic_transition():
    """Test a typical combination of state variables."""
    state = SystemState(s=1.0, m=2.0, v=3.0, l=4.0, p=5.0, r=6.0, z=7.0)
    expected = (
        1.0
        + 0.5 * 2.0
        + 0.3 * 3.0
        - 0.2 * 4.0
        + 0.4 * 5.0
        + 0.6 * 6.0
        - 0.3 * 7.0
    )
    result = F(state)
    assert math.isclose(result, expected, rel_tol=1e-9)

def test_zero_inputs():
    """All zero inputs should result in zero output."""
    state = SystemState(s=0, m=0, v=0, l=0, p=0, r=0, z=0)
    assert F(state) == 0

@pytest.mark.parametrize(
    "state, expected",
    [
        (SystemState(0, 1, 0, 0, 0, 0, 0), 0.5),  # environment only
        (SystemState(0, 0, 1, 0, 0, 0, 0), 0.3),  # vectors only
        (SystemState(0, 0, 0, 1, 0, 0, 0), -0.2),  # latencies only
        (SystemState(0, 0, 0, 0, 1, 0, 0), 0.4),  # pressures only
        (SystemState(0, 0, 0, 0, 0, 1, 0), 0.6),  # memory only
        (SystemState(0, 0, 0, 0, 0, 0, 1), -0.3),  # hidden variables only
    ],
)
def test_single_factor_effects(state, expected):
    """Verify that each factor contributes correctly when isolated."""
    result = F(state)
    assert math.isclose(result, expected, rel_tol=1e-9)