XOR r0, r0, r0
@loop
IN r1, 0
IN r2, 1
ADD r3, r1, r2
OUT 3, r3
BZ r0, @loop