package com.vectras.vm.rafaelia;

/**
 * Matriz direcional determinística RAFAELIA: 8 áreas metodológicas × 7 direções complementares.
 */
public final class RafaeliaDirectionalMatrix {

    public static final int DIRECTION_INPUT = 1;
    public static final int DIRECTION_OUTPUT = 2;
    public static final int DIRECTION_STORAGE = 3;
    public static final int DIRECTION_PROCESSING = 4;
    public static final int DIRECTION_INFERENCE = 5;
    public static final int DIRECTION_CONTROL = 6;
    public static final int DIRECTION_AUDIT = 7;

    public static final int AREA_COUNT = 8;
    public static final int DIRECTION_COUNT = 7;

    public static final class AreaSpec {
        public final int pathId;
        public final String symbolicLabel;
        public final String technicalLabel;
        public final int primaryDirectionId;

        AreaSpec(int pathId, String symbolicLabel, String technicalLabel, int primaryDirectionId) {
            this.pathId = pathId;
            this.symbolicLabel = symbolicLabel;
            this.technicalLabel = technicalLabel;
            this.primaryDirectionId = primaryDirectionId;
        }
    }

    public static final class DirectionSpec {
        public final int directionId;
        public final String symbolicLabel;
        public final String technicalLabel;

        DirectionSpec(int directionId, String symbolicLabel, String technicalLabel) {
            this.directionId = directionId;
            this.symbolicLabel = symbolicLabel;
            this.technicalLabel = technicalLabel;
        }
    }

    private static final DirectionSpec[] DIRECTIONS = {
        null,
        new DirectionSpec(DIRECTION_INPUT, "↘IN", "input"),
        new DirectionSpec(DIRECTION_OUTPUT, "↗OUT", "output"),
        new DirectionSpec(DIRECTION_STORAGE, "↧STORE", "storage"),
        new DirectionSpec(DIRECTION_PROCESSING, "⟳PROC", "processing"),
        new DirectionSpec(DIRECTION_INFERENCE, "∴INF", "inference"),
        new DirectionSpec(DIRECTION_CONTROL, "⌘CTRL", "control"),
        new DirectionSpec(DIRECTION_AUDIT, "☍AUD", "audit")
    };

    private static final AreaSpec[] AREAS = {
        null,
        new AreaSpec(RafaeliaMethodPaths.PATH_INIT, "ψ-INIT", "kernel_bootstrap", DIRECTION_CONTROL),
        new AreaSpec(RafaeliaMethodPaths.PATH_OBSERVE, "χ-OBSERVE", "hardware_observability", DIRECTION_INPUT),
        new AreaSpec(RafaeliaMethodPaths.PATH_DENOISE, "ρ-DENOISE", "io_sanitization", DIRECTION_INPUT),
        new AreaSpec(RafaeliaMethodPaths.PATH_TRANSMUTE, "Δ-TRANSMUTE", "resource_routing", DIRECTION_PROCESSING),
        new AreaSpec(RafaeliaMethodPaths.PATH_MEMORY, "Σ-MEMORY", "persistent_coherence", DIRECTION_STORAGE),
        new AreaSpec(RafaeliaMethodPaths.PATH_COMPLETE, "Ω-COMPLETE", "termination_integrity", DIRECTION_OUTPUT),
        new AreaSpec(RafaeliaMethodPaths.PATH_SPIRAL, "√3/2-SPIRAL", "geometric_scan", DIRECTION_INFERENCE),
        new AreaSpec(RafaeliaMethodPaths.PATH_COHERENCE, "Φ-COHERENCE", "system_integrity", DIRECTION_AUDIT)
    };

    /**
     * 8 linhas (pathId 1..8) × 7 colunas (directionId 1..7).
     * Valor 1 indica vínculo metodológico ativo.
     */
    private static final int[][] MATRIX = {
        {},
        {1, 0, 0, 1, 0, 1, 1}, // ψ INIT
        {1, 1, 0, 1, 1, 0, 1}, // χ OBSERVE
        {1, 1, 1, 1, 0, 0, 1}, // ρ DENOISE
        {1, 1, 1, 1, 1, 1, 0}, // Δ TRANSMUTE
        {1, 1, 1, 1, 0, 1, 1}, // Σ MEMORY
        {0, 1, 1, 1, 1, 1, 1}, // Ω COMPLETE
        {1, 1, 0, 1, 1, 1, 1}, // √3/2 SPIRAL
        {1, 1, 1, 1, 1, 1, 1}  // Φ COHERENCE
    };

    private RafaeliaDirectionalMatrix() {}

    public static AreaSpec areaByPath(int pathId) {
        return (pathId >= 1 && pathId <= AREA_COUNT) ? AREAS[pathId] : null;
    }

    public static DirectionSpec directionById(int directionId) {
        return (directionId >= 1 && directionId <= DIRECTION_COUNT) ? DIRECTIONS[directionId] : null;
    }

    public static int relation(int pathId, int directionId) {
        if (pathId < 1 || pathId > AREA_COUNT || directionId < 1 || directionId > DIRECTION_COUNT) {
            return 0;
        }
        return MATRIX[pathId][directionId - 1];
    }

    public static int[][] matrix() {
        int[][] copy = new int[MATRIX.length][];
        for (int i = 0; i < MATRIX.length; i++) {
            copy[i] = MATRIX[i].clone();
        }
        return copy;
    }
}
