package com.vectras.vm.rafaelia;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link RafaeliaMethodPaths} and {@link RafaeliaPathValidator} contracts.
 * Note: Full validator.validate(ctx) requires Android context; tested separately as instrumented test.
 */
public class RafaeliaPathValidatorTest {

    @Test
    public void allEightPathConstants_areDefinedAndDistinct() {
        int[] paths = {
            RafaeliaMethodPaths.PATH_INIT,
            RafaeliaMethodPaths.PATH_OBSERVE,
            RafaeliaMethodPaths.PATH_DENOISE,
            RafaeliaMethodPaths.PATH_TRANSMUTE,
            RafaeliaMethodPaths.PATH_MEMORY,
            RafaeliaMethodPaths.PATH_COMPLETE,
            RafaeliaMethodPaths.PATH_SPIRAL,
            RafaeliaMethodPaths.PATH_COHERENCE,
        };
        // All are between 1-8
        for (int p : paths) {
            Assert.assertTrue("path must be 1..8: " + p, p >= 1 && p <= 8);
            Assert.assertTrue(RafaeliaMethodPaths.isValid(p));
        }
        // All are distinct
        for (int i = 0; i < paths.length; i++) {
            for (int j = i + 1; j < paths.length; j++) {
                Assert.assertNotEquals("Duplicate path constants", paths[i], paths[j]);
            }
        }
    }

    @Test
    public void labelReturnsKnownStringForEachPath() {
        for (int p = 1; p <= 8; p++) {
            String label = RafaeliaMethodPaths.label(p);
            Assert.assertNotNull(label);
            Assert.assertFalse(label.isEmpty());
            Assert.assertFalse("Unknown path should not appear for valid id",
                label.startsWith("PATH_UNKNOWN"));
        }
    }

    @Test
    public void labelReturnsUnknownForInvalidPath() {
        Assert.assertTrue(RafaeliaMethodPaths.label(0).contains("UNKNOWN"));
        Assert.assertTrue(RafaeliaMethodPaths.label(9).contains("UNKNOWN"));
        Assert.assertTrue(RafaeliaMethodPaths.label(-1).contains("UNKNOWN"));
    }

    @Test
    public void cycleSymbols_coverAllPaths() {
        String[] expectedSymbols = { "ψ", "χ", "ρ", "Δ", "Σ", "Ω", "√3/2", "Φ" };
        for (int i = 0; i < expectedSymbols.length; i++) {
            int pathId = i + 1;
            Assert.assertEquals(expectedSymbols[i], RafaeliaMethodPaths.cycleSymbol(pathId));
        }
    }

    @Test
    public void spiralPath_deterministicComputation() {
        // PATH 7 spiral state must be non-zero (deterministic math check, no Context needed)
        final long PHI32   = 0x9E3779B9L;
        final long SQRT3_2 = 0xDDB3D743L;
        long state = 0x633L;
        for (int i = 0; i < 42; i++) {
            state = (state * PHI32) & 0xFFFFFFFFL;
            state = (state ^ (state >>> 16)) & 0xFFFFFFFFL;
            state = (state * SQRT3_2) & 0xFFFFFFFFL;
        }
        Assert.assertNotEquals("Spiral must not reduce to zero", 0L, state);
        Assert.assertNotEquals("Spiral must not reduce to max", 0xFFFFFFFFL, state);
    }

    @Test
    public void allLabels_areNonEmptyAndUnique() {
        java.util.Set<String> labels = new java.util.HashSet<>();
        for (int p = 1; p <= 8; p++) {
            String label = RafaeliaMethodPaths.label(p);
            Assert.assertFalse("Label must not be empty", label.isEmpty());
            Assert.assertTrue("Label must be unique: " + label, labels.add(label));
        }
    }

    @Test
    public void directionalMatrix_hasNoOrphanIdsAndNoMissingDirection() {
        for (int pathId = 1; pathId <= RafaeliaDirectionalMatrix.AREA_COUNT; pathId++) {
            RafaeliaDirectionalMatrix.AreaSpec area = RafaeliaDirectionalMatrix.areaByPath(pathId);
            Assert.assertNotNull("Area must exist for path " + pathId, area);
            Assert.assertEquals(pathId, area.pathId);
            Assert.assertTrue(
                "Primary direction must be valid for path " + pathId,
                area.primaryDirectionId >= 1
                    && area.primaryDirectionId <= RafaeliaDirectionalMatrix.DIRECTION_COUNT
            );

            int active = 0;
            for (int dirId = 1; dirId <= RafaeliaDirectionalMatrix.DIRECTION_COUNT; dirId++) {
                int rel = RafaeliaDirectionalMatrix.relation(pathId, dirId);
                Assert.assertTrue("Relation must be binary", rel == 0 || rel == 1);
                if (rel == 1) {
                    active++;
                }
            }
            Assert.assertTrue("No direction linked for path " + pathId, active > 0);
            Assert.assertEquals(
                "Primary direction relation must be active for path " + pathId,
                1,
                RafaeliaDirectionalMatrix.relation(pathId, area.primaryDirectionId)
            );
        }
    }

    @Test
    public void methodPaths_exposesAreaAndPrimaryDirectionBinding() {
        for (int pathId = 1; pathId <= 8; pathId++) {
            String areaSymbolic = RafaeliaMethodPaths.areaSymbolicLabel(pathId);
            String areaTechnical = RafaeliaMethodPaths.areaTechnicalLabel(pathId);
            int primaryDirectionId = RafaeliaMethodPaths.primaryDirectionId(pathId);
            String directionTechnical = RafaeliaMethodPaths.primaryDirectionTechnicalLabel(pathId);

            Assert.assertNotNull(areaSymbolic);
            Assert.assertNotEquals("?", areaSymbolic);
            Assert.assertNotNull(areaTechnical);
            Assert.assertFalse(areaTechnical.startsWith("AREA_UNKNOWN"));
            Assert.assertTrue(primaryDirectionId >= 1 && primaryDirectionId <= 7);
            Assert.assertNotNull(directionTechnical);
            Assert.assertFalse(directionTechnical.startsWith("DIRECTION_UNKNOWN"));
        }
    }

    @Test
    public void directionalEvidence_containsAllDirectionKeys() {
        String evidence = RafaeliaPathValidator.directionalEvidence(
            RafaeliaMethodPaths.PATH_COHERENCE,
            true,
            true,
            true,
            true,
            true,
            true,
            true
        );
        Assert.assertTrue(evidence.contains("input="));
        Assert.assertTrue(evidence.contains("output="));
        Assert.assertTrue(evidence.contains("storage="));
        Assert.assertTrue(evidence.contains("processing="));
        Assert.assertTrue(evidence.contains("inference="));
        Assert.assertTrue(evidence.contains("control="));
        Assert.assertTrue(evidence.contains("audit="));
        Assert.assertTrue(evidence.contains("/m1"));
    }
}
