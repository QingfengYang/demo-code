#!/usr/bin/env python3

from rb_sort import SortUtil
count = 0

class QuickSort_Lomuto:

    @staticmethod
    def sort(arr: [], lo: int, hi: int):
        '''
        global count
        count = count + 1
        print("COUNT=%d : lo: %d, hi: %d" % (count, lo, hi))
        '''
        if lo < hi:
            SortUtil.randomPivot(arr, lo, hi)
            p = QuickSort_Lomuto.partition(arr, lo, hi)
            QuickSort_Lomuto.sort(arr, lo, p - 1)
            QuickSort_Lomuto.sort(arr, p + 1, hi)


    @staticmethod
    def partition(arr: [], lo: int, hi: int):
        key = arr[hi]
        i = lo
        for j in range(lo, hi):
            if arr[j] < key:
                if i != j:
                    tmp = arr[i]
                    arr[i] = arr[j]
                    arr[j] = tmp
                i = i + 1
        arr[hi] = arr[i]
        arr[i] = key
        return i



class QuickSort_Hoare:

    @staticmethod
    def sort(arr:[], lo: int, hi: int):
        if lo < hi:
            SortUtil.randomPivot(arr, lo, hi)
            p = QuickSort_Hoare.partition(arr, lo, hi)
            QuickSort_Hoare.sort(arr, lo, p - 1)
            QuickSort_Hoare.sort(arr, p + 1, hi)

    @staticmethod
    def partition(arr: [], lo: int, hi: int):
        key = arr[lo]
        i = lo
        j = hi

        while i < j:
            while arr[i] < key:
                i = i + 1

            while arr[j] > key:
                j = j - 1

            if i < j:
                tmp = arr[i]
                arr[i] = arr[j]
                arr[j] = tmp
        return j


if __name__ == '__main__':
    arr: [int] = [2, 1]
    pivot = QuickSort_Hoare.partition(arr, 0, 1)
    print("Pivot: %d" % pivot)
    print("arr: %s" % arr)