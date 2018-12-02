#!/usr/bin/env python3

import random
import time
from rb_sort.Insort_sort import RobertInsertSort
from rb_sort.Insort_sort import DefaultInsertSorter
from rb_sort.MergeSort import MergeSort
from rb_sort.QuickSort import QuickSort_Lomuto
from rb_sort.QuickSort import QuickSort_Hoare
from rb_sort.ComplexSort import ComplexSort

def test_sort():

    TEST_RANGE = 100000000
    TEST_SIZE = 5000000

    test_data: [int] = []
    data_set: set(int) = set()
    for i in range(TEST_SIZE):
        item = random.randint(0, TEST_RANGE)
        while item in data_set:
            item = random.randint(0, TEST_RANGE)
        else:
            data_set.add(item)

    test_data.extend(data_set)
    #print(test_data)
    print("Test data size: %d" % len(test_data))
    '''
    arr1 = test_data.copy()
    t1_start = int(round(time.time() * 1000))
    RobertInsertSort.sort(arr1)
    t1_end = int(round(time.time() * 1000))
    print("Robert insert Sort: %d" % (t1_end - t1_start))
    print(arr1)

    arr2 = test_data.copy()
    t2_start = int(round(time.time() * 1000))
    DefaultInsertSorter.sort(arr2)
    t2_end = int(round(time.time() * 1000))
    print("Insert Sort: %d" % (t2_end - t2_start))
    print(arr2)
    '''

    arr3 = test_data.copy()
    t3_start = int(round(time.time() * 1000))
    MergeSort.merge_sort(arr3, 0, len(arr3) - 1)
    t3_end = int(round(time.time() * 1000))
    print("Mrege Sort: %d" % (t3_end - t3_start))
    #print(arr3)

    arr4 = test_data.copy()
    t4_start = int(round(time.time() * 1000))
    QuickSort_Lomuto.sort(arr4, 0, len(arr4) - 1)
    t4_end = int(round(time.time() * 1000))
    print("QuickSort_Lomuto Sort: %d" % (t4_end - t4_start))
    #print(arr4)

    arr5 = test_data.copy()
    t5_start = int(round(time.time() * 1000))
    QuickSort_Hoare.sort(arr5, 0, len(arr5) - 1)
    t5_end = int(round(time.time() * 1000))
    print("QuickSort_Hoare Sort: %d" % (t5_end - t5_start))
    #print(arr5)

    arr6 = test_data.copy()
    complexSort = ComplexSort(20)
    t6_start = int(round(time.time() * 1000))
    complexSort.sort(arr6, 0, len(arr6) - 1)
    t6_end = int(round(time.time() * 1000))
    print("Complex Sort: %d" % (t6_end - t6_start))
    #print(arr6)



def test_merge():
    arr3 = [3, 1]
    print(arr3)
    MergeSort.merge(arr3, 0, 0, 1)
    print(arr3)


def test_find_pos():
    a = [1, 9, 3, 5]
    sorter = RobertInsertSort()
    for i in range(0, 4):
        print("%d: %d --> %d" % (i, a[i], sorter.find_pos(a, i)))

if __name__ == '__main__':
    #test_find_pos()
    #a = [1, 9, 3, 5]
    #flip_sort(a, 1, 2)
    #print(a)
    #test_merge()
    test_sort()