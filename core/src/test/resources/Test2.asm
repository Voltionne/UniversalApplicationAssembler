/*
Fibonacci generator
*/

// Start address of the fibonacci sequence (then will be do -1)
WLOW r0, 255
WUPP r0, 255

//Number of Fibonacci iterations
WLOW r1, 255
WUPP r1, 127

// The first fibonacci number
WLOW r2, 0
WUPP r2, 0

// The second fibonacci number
WLOW r3, 1
WUPP r3, 0

// Counter (for counting iterations)
WLOW r4, 0
WUPP r4, 0

// Constant one
WLOW r5, 1
WUPP r5, 0

// Pointer to the start of the loop
WLOW p0, @loop
WUPP p0, 0

@loop
    //Perform fibonacci
    COPY r2, alu-a
    COPY r3, alu-b
    ALU add
    COPY r3, r2
    COPY alu-r, r3

    //Save result to RAM
    WRAM r0, r2

    //Decrease address by 1
    COPY r0, alu-a
    COPY r5, alu-b
    ALU sub
    COPY alu-r, r0

    //Increase iteration counter by 1
    COPY r4, alu-a
    ALU add
    COPY alu-r, r4

    CJUMP r4, less, r1, 0
HALT