WLOW r0, 255
WUPP r0, 255
WLOW r1, 255
WUPP r1, 127
WLOW r2, 0
WUPP r2, 0
WLOW r3, 1
WUPP r3, 0
WLOW r4, 0
WUPP r4, 0
WLOW r5, 1
WUPP r5, 0
WLOW p0, 14
WUPP p0, 0
COPY r2, alu-a
COPY r3, alu-b
ALU add
COPY r3, r2
COPY alu-r, r3
WRAM r0, r2
COPY r0, alu-a
COPY r5, alu-b
ALU sub
COPY alu-r, r0
COPY r4, alu-a
ALU add
COPY alu-r, r4
CJUMP r4, less, r1, 0
HALT