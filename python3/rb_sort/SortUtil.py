#!/usr/bin/env python3

import random

def randomPivot(arr: [], lo: int, hi: int):
    if hi - lo > 3:
        pivot_index = random.randint(lo, hi)
        tmp = arr[hi]
        arr[hi] = arr[pivot_index]
        arr[pivot_index] = tmp