#!/usr/bin/env python3

import random
import time
from rb_sort.insort_sort import RobertInsertSort
from rb_sort.insort_sort import DefaultInsertSorter
from rb_sort.MergeSort import MergeSort

def test_sort():

    TEST_RANGE = 10
    TEST_SIZE = 20

    test_data: [int] = []
    data_set: set(int) = set()
    for i in range(TEST_SIZE):
        item = random.randint(0, TEST_RANGE)
        while item in data_set:
            item = random.randint(0, TEST_RANGE)
        else:
            data_set.add(item)

    test_data.extend(data_set)
    print(test_data)
    '''
    arr1 = test_data.copy()
    t1_start = int(round(time.time() * 1000))
    RobertInsertSort.sort(arr1)
    t1_end = int(round(time.time() * 1000))
    print("Robert insert Sort: %d" % (t1_start - t1_end))
    print(arr1)

    arr2 = test_data.copy()
    t2_start = int(round(time.time() * 1000))
    DefaultInsertSorter.sort(arr2)
    t2_end = int(round(time.time() * 1000))
    print("Insert Sort: %d" % (t2_end - t2_start))
    print(arr2)
    '''
    arr3 = test_data.copy()
    #arr3 = [3, 1, 2]
    #print(arr3)
    t3_start = int(round(time.time() * 1000))
    MergeSort.merge_sort(arr3, 0, len(arr3) - 1)
    t3_end = int(round(time.time() * 1000))
    print("Mrege Sort: %d" % (t3_start - t3_end))

    print(arr3)


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