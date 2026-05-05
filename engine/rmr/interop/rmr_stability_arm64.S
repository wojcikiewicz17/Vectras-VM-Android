.text
.align 6
.global rmr_stability_step_arm64
.type rmr_stability_step_arm64, %function
// x0 = pointer to state[4] (s0..s3)
// x1 = pointer to fault event byte stream
// x2 = number of events
// returns x0 = stability score (0..2^63-1)
rmr_stability_step_arm64:
    mov x9, x0
    ldp x10, x11, [x9]
    ldp x12, x13, [x9, #16]
    mov x14, x1
    mov x15, x2
1:
    cbz x15, 2f
    ldrb w16, [x14], #1
    eor x10, x10, x11
    add x11, x11, x16
    ror x12, x12, #7
    add x12, x12, x10
    eor x13, x13, x12
    eor x17, x13, x12
    add x10, x10, x17
    sub x15, x15, #1
    b 1b
2:
    stp x10, x11, [x9]
    stp x12, x13, [x9, #16]
    eor x0, x10, x11
    add x0, x0, x12
    eor x0, x0, x13
    and x0, x0, #0x7fffffffffffffff
    ret
.size rmr_stability_step_arm64, .-rmr_stability_step_arm64
