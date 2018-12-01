#!/usr/bin/env python
# encoding: utf-8


class RobertInsertSort:

    @staticmethod
    def flip_sort(arr: [], correct_pos: int, item_index: int):
        tmp: int = arr[item_index]
        for i in range(item_index, correct_pos, -1):
            arr[i] = arr[i - 1]

        arr[correct_pos] = tmp

    @staticmethod
    def find_pos(sub_arr: [], item_index: int):
        correct_pos = item_index
        for pos in range(item_index - 1, -1, -1):
            if sub_arr[pos] > sub_arr[item_index]:
                correct_pos = pos
            else:
                break
        return correct_pos

    @staticmethod
    def sort(arr: []):
        for i in range(1, len(arr)):
            correct_pos = RobertInsertSort.find_pos(arr, i)
            RobertInsertSort.flip_sort(arr, correct_pos, i)


class DefaultInsertSorter:

    @staticmethod
    def sort(arr: [int]):
        for i in range(1, len(arr)):
            key = arr[i]
            j = i - 1
            while j >= 0 and arr[j] > key:
                arr[j + 1] = arr[j]
                j = j - 1
            arr[j + 1] = key

