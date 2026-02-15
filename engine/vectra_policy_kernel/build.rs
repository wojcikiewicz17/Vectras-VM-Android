fn main() {
    let mut build = cc::Build::new();
    build
        .include("../rmr/include")
        .file("../rmr/src/rmr_unified_kernel.c")
        .file("../rmr/src/rmr_hw_detect.c")
        .file("../rmr/src/rmr_cycles.c")
        .file("../rmr/src/rmr_ll_ops.c")
        .warnings(false)
        .compile("rmr_unified_kernel");

    println!("cargo:rerun-if-changed=../rmr/include/rmr_unified_kernel.h");
    println!("cargo:rerun-if-changed=../rmr/src/rmr_unified_kernel.c");
    println!("cargo:rerun-if-changed=../rmr/src/rmr_hw_detect.c");
    println!("cargo:rerun-if-changed=../rmr/src/rmr_cycles.c");
    println!("cargo:rerun-if-changed=../rmr/src/rmr_ll_ops.c");
}
