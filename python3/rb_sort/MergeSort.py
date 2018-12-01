#!/usr/bin/env python
# encoding: utf-8

import sys

class BadBoundary(Exception):

    def __init__(self, msg):
        super(BadBoundary, self).__init__(msg)


class MergeSort:

    # sort arr including [start_index, end_index]
    @staticmethod
    def merge_sort(arr: [int], start_index: int, end_index: int):
        print("start: %d, end: %d" % (start_index, end_index))
        if end_index == start_index:
            return
        mid = int((start_index + end_index)/2)
        # sort left part: the result write back
        MergeSort.merge_sort(arr, start_index, mid)
        # sort right part
        MergeSort.merge_sort(arr, mid + 1, end_index)
        MergeSort.merge(arr, start_index, mid, end_index)


    # left_start <= mid; mid < right_end
    @staticmethod
    def merge(arr: [], left_start: int, mid: int, right_end: int):
        left_part = arr[left_start: mid + 1]
        left_part.append(sys.maxsize)
        right_part = arr[mid + 1: right_end + 1]
        right_part.append(sys.maxsize)
        l_pos = 0
        r_pos = 0
        for i in range(left_start, right_end + 1):
            if left_part[l_pos] <= right_part[r_pos]:
                arr[i] = left_part[l_pos]
                l_pos = l_pos + 1
            else:
                arr[i] = right_part[r_pos]
                r_pos = r_pos + 1

