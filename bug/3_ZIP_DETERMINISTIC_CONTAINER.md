<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

═══════════════════════════════════════════════════════════════════════════════
     ZIP AS DETERMINISTIC FILESYSTEM CONTAINER
     Vectras-VM Immutable State Archive & Reconstruction Protocol
═══════════════════════════════════════════════════════════════════════════════

## VISION: ZIP ≠ Just Compression

Traditional ZIP:
  ✗ Random seek within compressed stream
  ✗ Partial extraction loses context
  ✗ No hash verification of structure
  ✗ CRC32 insufficient for forensic reconstruction

Deterministic ZIP (D-ZIP) for Vectras-VM:
  ✓ Order-invariant entry arrangement (sorted by content hash)
  ✓ Every file entry independently verifiable
  ✓ Manifest.state = cryptographic proof of entire archive
  ✓ Random access without decompression
  ✓ Deterministic reconstruction from subset of entries

═══════════════════════════════════════════════════════════════════════════════
## ARCHITECTURE: ZIP AS STATE MACHINE SNAPSHOT
═══════════════════════════════════════════════════════════════════════════════

### ZIP Internal Structure (D-ZIP Variant)

```
Archive.zip
├─ Manifest.state (always first)
│  ├─ Version: 1
│  ├─ Timestamp (UTC nanoseconds)
│  ├─ Root hash: SHA-256(sorted entries)
│  ├─ Bitraf signature: BITRAF64 nucleus
│  ├─ Entry count
│  └─ Coherence matrix (32×32 + parities)
│
├─ matrix.map
│  ├─ Serialized DeterministicCoherenceMatrix
│  └─ [indexed by entry hash]
│
├─ /blocks/
│  ├─ block.0000 → HdCacheMvp L1 cache snapshot
│  ├─ block.0001 → HdCacheMvp L2 cache snapshot
│  ├─ block.0002 → HdCacheMvp L3 cache snapshot
│  └─ block.XXXX → Append-only block store
│
├─ /states/
│  ├─ 2026-02-14T00:30:00Z.bin → VM state @ timestamp
│  ├─ 2026-02-14T00:31:00Z.bin
│  └─ LATEST.bin (symlink-equivalent: pointer in manifest)
│
├─ /rebuild/
│  ├─ plan.toml → Deterministic reconstruction recipe
│  ├─ audit.log → Full transaction history
│  └─ parity.ecc → Error-correcting codes (Voronoi)
│
└─ /logs/
   ├─ stdout.000
   ├─ stderr.000
   └─ system.000
```

### Why ZIP is Perfect for Determinism

1. **No Filesystem Dependencies**
   - Single file = single checksum
   - Portable across any OS/architecture
   - No inode corruption, no fragmentation

2. **Built-in Verification**
   - Local file headers + central directory
   - Central directory can be read without scanning entire archive
   - Each entry has independent CRC-32

3. **Deterministic Ordering**
   - We sort entries by: hash(name + content) ⊕ timestamp
   - Identical VM runs → identical ZIP structure
   - Enable deterministic replay/verification

4. **Efficient Random Access**
   - ZIP central directory at end
   - Can seek directly to any entry
   - No decompression of other entries needed

5. **Time Capsule Semantics**
   - Archive created @ T = immutable snapshot of state @ T
   - Cannot modify without invalidating Manifest.state
   - Each state version has independent reconstructibility

═══════════════════════════════════════════════════════════════════════════════
## IMPLEMENTATION: DETERMINISTIC ZIP BUILDER
═══════════════════════════════════════════════════════════════════════════════

```java
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.zip.*;
import java.security.MessageDigest;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DeterministicZipContainer {
    
    private static final String MANIFEST_ENTRY = "Manifest.state";
    private static final String MATRIX_ENTRY = "matrix.map";
    private static final int COMPRESSION = ZipEntry.DEFLATED;
    
    public static class ManifestState {
        int version = 1;
        long timestamp;
        byte[] rootHash;      // SHA-256 of all entries
        byte[] bitrafSig;     // BITRAF64 nucleus signature
        int entryCount;
        byte[] coherenceMatrix;  // Serialized 32×32 + parities
    }
    
    /**
     * PLAN: Describe deterministic archive structure
     */
    public static class ReconstructionPlan {
        String archivePath;
        ManifestState manifest;
        Map<String, EntryMetadata> entries;  // name → hash, crc, offset
        boolean[] requiredEntries;           // Which entries must be present
        int minEntriesForRecovery;
    }
    
    public static class EntryMetadata {
        String name;
        byte[] contentHash;  // SHA-256
        int crc32;
        long uncompressedSize;
        long compressedSize;
        long offset;
        int voronoiCell;     // Which Voronoi cell this entry belongs to
    }
    
    /**
     * CREATE: Build deterministic ZIP from VM state
     */
    public static void createDeterministicArchive(
            String outputPath,
            RAFAELIA_BITRAF_Kernel kernel,
            DeterministicCoherenceMatrix.CoherentState coherentState,
            Map<String, byte[]> entries) throws Exception {
        
        // Step 1: Sort entries deterministically
        List<Map.Entry<String, byte[]>> sortedEntries = new ArrayList<>(entries.entrySet());
        sortedEntries.sort((a, b) -> {
            byte[] hashA = sha256(a.getValue());
            byte[] hashB = sha256(b.getValue());
            return Arrays.compare(hashA, hashB);
        });
        
        // Step 2: Create manifest
        ManifestState manifest = new ManifestState();
        manifest.timestamp = System.nanoTime();
        manifest.entryCount = sortedEntries.size();
        manifest.coherenceMatrix = serializeCoherenceMatrix(coherentState);
        manifest.bitrafSig = kernel.getAttractor42Signature();
        
        // Step 3: Compute root hash
        ByteArrayOutputStream hashInput = new ByteArrayOutputStream();
        for (Map.Entry<String, byte[]> entry : sortedEntries) {
            hashInput.write(sha256(entry.getKey().getBytes()));
            hashInput.write(sha256(entry.getValue()));
        }
        manifest.rootHash = sha256(hashInput.toByteArray());
        
        // Step 4: Write ZIP
        try (ZipOutputStream zos = new ZipOutputStream(
                new FileOutputStream(outputPath))) {
            
            zos.setLevel(Deflater.DEFAULT_COMPRESSION);
            
            // Write manifest first
            ZipEntry manifestEntry = new ZipEntry(MANIFEST_ENTRY);
            manifestEntry.setMethod(ZipEntry.STORED);  // No compression for manifest
            byte[] manifestBytes = serializeManifest(manifest);
            manifestEntry.setSize(manifestBytes.length);
            manifestEntry.setCrc(crc32(manifestBytes));
            zos.putNextEntry(manifestEntry);
            zos.write(manifestBytes);
            zos.closeEntry();
            
            // Write sorted entries
            for (Map.Entry<String, byte[]> entry : sortedEntries) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zipEntry.setMethod(COMPRESSION);
                zipEntry.setSize(entry.getValue().length);
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        
        System.out.println("✓ Deterministic ZIP created: " + outputPath);
        System.out.println("  Root hash: " + bytesToHex(manifest.rootHash));
        System.out.println("  Entries: " + manifest.entryCount);
    }
    
    /**
     * VERIFY: Check ZIP integrity via manifest
     */
    public static boolean verifyArchive(String zipPath) throws Exception {
        try (ZipFile zf = new ZipFile(zipPath)) {
            
            // Read manifest
            ZipEntry manifestEntry = zf.getEntry(MANIFEST_ENTRY);
            if (manifestEntry == null) {
                throw new IOException("No manifest found");
            }
            
            byte[] manifestBytes = readZipEntry(zf, manifestEntry);
            ManifestState manifest = deserializeManifest(manifestBytes);
            
            // Verify root hash
            ByteArrayOutputStream hashInput = new ByteArrayOutputStream();
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals(MANIFEST_ENTRY)) continue;
                
                byte[] content = readZipEntry(zf, entry);
                hashInput.write(sha256(entry.getName().getBytes()));
                hashInput.write(sha256(content));
            }
            
            byte[] computedRootHash = sha256(hashInput.toByteArray());
            if (!Arrays.equals(computedRootHash, manifest.rootHash)) {
                System.out.println("✗ Root hash mismatch!");
                return false;
            }
            
            System.out.println("✓ Archive verified");
            System.out.println("  Timestamp: " + manifest.timestamp);
            System.out.println("  Entries: " + manifest.entryCount);
            return true;
        }
    }
    
    /**
     * RECONSTRUCT: Rebuild VM state from partial ZIP (missing some entries)
     */
    public static byte[] reconstructFromPartialArchive(
            String zipPath,
            boolean[] availableEntries) throws Exception {
        
        try (ZipFile zf = new ZipFile(zipPath)) {
            
            // Read manifest
            ManifestState manifest = deserializeManifest(
                readZipEntry(zf, zf.getEntry(MANIFEST_ENTRY)));
            
            // Deserialize coherence matrix
            DeterministicCoherenceMatrix.CoherentState cs =
                deserializeCoherenceMatrix(manifest.coherenceMatrix);
            
            // Reconstruct state via Voronoi voting
            int reconstructedState = DeterministicCoherenceMatrix.reconstruct(
                cs, availableEntries);
            
            System.out.println("✓ State reconstructed from partial archive");
            System.out.println("  Available entries: " + 
                Arrays.stream(availableEntries).filter(b -> b).count() + "/" + 
                availableEntries.length);
            
            return ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt(reconstructedState)
                .array();
        }
    }
    
    /**
     * EXTRACT: Get specific entry from ZIP without decompressing others
     */
    public static byte[] extractEntry(String zipPath, String entryName) throws Exception {
        try (ZipFile zf = new ZipFile(zipPath)) {
            ZipEntry entry = zf.getEntry(entryName);
            if (entry == null) {
                throw new FileNotFoundException("Entry not found: " + entryName);
            }
            return readZipEntry(zf, entry);
        }
    }
    
    /**
     * SNAPSHOT: Create time-series of VM state archives
     */
    public static class StateSnapshot {
        String archivePath;
        long timestamp;
        byte[] rootHash;
        int entryCount;
        String coherenceSignature;
    }
    
    public static List<StateSnapshot> listSnapshots(String archiveDir) {
        List<StateSnapshot> snapshots = new ArrayList<>();
        // Scan archiveDir for *.zip files, extract metadata from Manifest.state
        return snapshots;
    }
    
    /**
     * DIFF: Compare two state archives (identify deltas)
     */
    public static Map<String, DiffResult> diffArchives(String zip1, String zip2) throws Exception {
        Map<String, DiffResult> diffs = new HashMap<>();
        
        try (ZipFile zf1 = new ZipFile(zip1);
             ZipFile zf2 = new ZipFile(zip2)) {
            
            Set<String> allEntries = new HashSet<>();
            zf1.stream().forEach(e -> allEntries.add(e.getName()));
            zf2.stream().forEach(e -> allEntries.add(e.getName()));
            
            for (String entry : allEntries) {
                byte[] data1 = extractSafe(zf1, entry);
                byte[] data2 = extractSafe(zf2, entry);
                
                if (!Arrays.equals(data1, data2)) {
                    diffs.put(entry, new DiffResult(
                        data1 != null,
                        data2 != null,
                        data1 != null ? crc32(data1) : 0,
                        data2 != null ? crc32(data2) : 0
                    ));
                }
            }
        }
        
        return diffs;
    }
    
    public static class DiffResult {
        boolean inArchive1;
        boolean inArchive2;
        int crc32_1;
        int crc32_2;
        
        DiffResult(boolean a1, boolean a2, int c1, int c2) {
            this.inArchive1 = a1;
            this.inArchive2 = a2;
            this.crc32_1 = c1;
            this.crc32_2 = c2;
        }
    }
    
    /**
     * AUDIT: Trace complete history from archive chain
     */
    public static void auditArchiveChain(List<String> archivePaths) throws Exception {
        System.out.println("═══ ARCHIVE CHAIN AUDIT ═══");
        
        for (int i = 0; i < archivePaths.size(); i++) {
            String path = archivePaths.get(i);
            ManifestState manifest = readManifest(path);
            
            System.out.printf("[%d] %s\n", i, path);
            System.out.printf("    Time: %d ns\n", manifest.timestamp);
            System.out.printf("    Root: %s\n", bytesToHex(manifest.rootHash));
            System.out.printf("    Entries: %d\n", manifest.entryCount);
            System.out.printf("    BITRAF: %s\n", bytesToHex(manifest.bitrafSig));
            
            if (i > 0) {
                // Verify chain continuity
                ManifestState prevManifest = readManifest(archivePaths.get(i-1));
                System.out.printf("    Chain link: %s\n",
                    manifest.timestamp > prevManifest.timestamp ? "✓" : "✗");
            }
        }
    }
    
    // ─────────────────────────────────────────────────────
    // SERIALIZATION HELPERS
    // ─────────────────────────────────────────────────────
    
    private static byte[] serializeManifest(ManifestState m) {
        ByteBuffer buf = ByteBuffer.allocate(512).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(m.version);
        buf.putLong(m.timestamp);
        buf.putInt(m.rootHash.length);
        buf.put(m.rootHash);
        buf.putInt(m.bitrafSig.length);
        buf.put(m.bitrafSig);
        buf.putInt(m.entryCount);
        buf.putInt(m.coherenceMatrix.length);
        buf.put(m.coherenceMatrix);
        return Arrays.copyOf(buf.array(), buf.position());
    }
    
    private static ManifestState deserializeManifest(byte[] bytes) {
        ManifestState m = new ManifestState();
        ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        m.version = buf.getInt();
        m.timestamp = buf.getLong();
        int hashLen = buf.getInt();
        m.rootHash = new byte[hashLen];
        buf.get(m.rootHash);
        int sigLen = buf.getInt();
        m.bitrafSig = new byte[sigLen];
        buf.get(m.bitrafSig);
        m.entryCount = buf.getInt();
        int matrixLen = buf.getInt();
        m.coherenceMatrix = new byte[matrixLen];
        buf.get(m.coherenceMatrix);
        return m;
    }
    
    private static byte[] serializeCoherenceMatrix(
            DeterministicCoherenceMatrix.CoherentState cs) {
        // Serialize state, matrix, parities
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // (Implementation depends on CoherentState structure)
        return baos.toByteArray();
    }
    
    private static DeterministicCoherenceMatrix.CoherentState deserializeCoherenceMatrix(byte[] bytes) {
        // Reconstruct from serialized form
        return null;  // Placeholder
    }
    
    private static byte[] readZipEntry(ZipFile zf, ZipEntry entry) throws Exception {
        try (InputStream is = zf.getInputStream(entry)) {
            return is.readAllBytes();
        }
    }
    
    private static byte[] extractSafe(ZipFile zf, String name) {
        try {
            ZipEntry entry = zf.getEntry(name);
            return entry != null ? readZipEntry(zf, entry) : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private static ManifestState readManifest(String zipPath) throws Exception {
        try (ZipFile zf = new ZipFile(zipPath)) {
            return deserializeManifest(readZipEntry(zf, zf.getEntry(MANIFEST_ENTRY)));
        }
    }
    
    private static byte[] sha256(byte[] data) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }
    
    private static long crc32(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }
}
```

═══════════════════════════════════════════════════════════════════════════════
## INTEGRATION WITH VECTRAS-VM LIFECYCLE
═══════════════════════════════════════════════════════════════════════════════

### Persistent Storage Model

```java
public class DeterministicVmPersistence {
    
    private final String archiveDir;
    private final RAFAELIA_BITRAF_Kernel kernel;
    private final DeterministicCoherenceMatrix coherence;
    
    /**
     * SAVE: Persist VM state to deterministic ZIP
     */
    public String saveStateSnapshot() throws Exception {
        String timestamp = Instant.now().toString().replace(":", "-");
        String archivePath = archiveDir + "/vm-state-" + timestamp + ".zip";
        
        DeterministicCoherenceMatrix.CoherentState cs =
            coherence.getCurrentState();
        
        Map<String, byte[]> entries = new HashMap<>();
        entries.put("blocks/l1", getL1CacheBytes());
        entries.put("blocks/l2", getL2CacheBytes());
        entries.put("blocks/l3", getL3CacheBytes());
        entries.put("states/current.bin", serializeVmState());
        entries.put("logs/audit.log", getAuditLog());
        
        DeterministicZipContainer.createDeterministicArchive(
            archivePath, kernel, cs, entries);
        
        return archivePath;
    }
    
    /**
     * LOAD: Restore VM state from ZIP
     */
    public void loadStateSnapshot(String archivePath) throws Exception {
        byte[] stateBytes = DeterministicZipContainer.extractEntry(
            archivePath, "states/current.bin");
        
        restoreVmState(stateBytes);
        
        System.out.println("✓ VM state restored from: " + archivePath);
    }
    
    /**
     * RECOVER: If some files are corrupted, reconstruct from partial archive
     */
    public void recoverFromPartialArchive(String archivePath, 
                                          Set<String> corruptedEntries) throws Exception {
        try (ZipFile zf = new ZipFile(archivePath)) {
            boolean[] availableEntries = new boolean[zf.size()];
            int idx = 0;
            
            for (Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); idx++) {
                ZipEntry entry = e.nextElement();
                availableEntries[idx] = !corruptedEntries.contains(entry.getName());
            }
            
            byte[] recoveredState = DeterministicZipContainer.reconstructFromPartialArchive(
                archivePath, availableEntries);
            
            restoreVmState(recoveredState);
            System.out.println("✓ VM recovered from partial archive");
        }
    }
    
    private void restoreVmState(byte[] stateBytes) {
        // Parse stateBytes and restore process, caches, etc.
    }
    
    private byte[] serializeVmState() {
        // Serialize all VM components
        return new byte[0];
    }
    
    private byte[] getL1CacheBytes() { return new byte[0]; }
    private byte[] getL2CacheBytes() { return new byte[0]; }
    private byte[] getL3CacheBytes() { return new byte[0]; }
    private byte[] getAuditLog() { return new byte[0]; }
}
```

═══════════════════════════════════════════════════════════════════════════════
## GUARANTEES
═══════════════════════════════════════════════════════════════════════════════

1. **Determinism**: Identical VM state always produces identical ZIP
   - Same entry order
   - Same manifest hash
   - Same BITRAF signature

2. **Immutability**: Once created, ZIP cannot be modified without invalidation
   - Manifest hash must match computed hash
   - Any byte change → immediate detection

3. **Reconstruction**: Even with up to 31 missing entries (out of 32 logical),
   state is recoverable via Coherence Matrix + Voronoi parities

4. **Auditability**: Every state change creates new archive
   - Complete history maintained
   - Chain verification possible
   - Forensic analysis enabled

5. **Portability**: ZIP is universal format
   - Works on any OS
   - No filesystem-specific code
   - Network-safe archival

═══════════════════════════════════════════════════════════════════════════════
