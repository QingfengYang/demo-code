#!/usr/bin/env python3

from rb_sort import SortUtil

class ComplexSort:

    def __init__(self, mark: int):
        self.mark = mark

    def sort(self, arr: [int], lo: int, hi: int):
        if hi - lo < self.mark:
            # insert sort
            self.insert_sort(arr, lo, hi)
        else:
            SortUtil.randomPivot(arr, lo, hi)
            p = self.partition(arr, lo, hi)
            self.sort(arr, lo, p - 1)
            self.sort(arr, p + 1, hi)

    def partition(self, arr: [int], lo: int, hi: int):
        i = lo - 1
        pivot = arr[hi]
        # j loop for [lo, hi)
        for j in range(lo, hi):
            if arr[j] < pivot:
                i = i + 1
                tmp = arr[i]
                arr[i] = arr[j]
                arr[j] = tmp
        tmp = arr[i + 1]
        arr[i + 1] = pivot
        arr[hi] = tmp
        return i + 1

    def insert_sort(self, arr: [int], lo: int, hi: int):
        for j in range(lo, hi + 1):
            curr = arr[j]
            i = j - 1
            while i > -1 and arr[i] > curr:
                arr[i+1] = arr[i]
                i = i - 1
            arr[i+1] = curr


if __name__ == "__main__":
    sorter = ComplexSort(2)
    testdata_arr = [
                    [1],
                    [1, 2],
                    [2, 1],
                    [1, 2, 3],
                    [1, 3, 2],
                    [2, 1, 3],
                    [3, 1, 2]
                    ]

    for arr in testdata_arr:
        print("data  : %s" % arr)
        sorter.sort(arr, 0, len(arr) - 1)
        print("sorted: %s\n" % arr)
