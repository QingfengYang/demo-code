#!/usr/bin/env python
# encoding: utf-8

def main():
    a = [1, 9, 3, 5]

    insert_sort(a)


def insert_sort(a: []):
    for i in a:
        print(i)


def flip_sort(arr: [], correct_pos: int, item_index: int):
    tmp: int = arr[item_index]
    for i in range(item_index, correct_pos, -1):
        arr[i] = arr[i - 1]

    arr[correct_pos] = tmp


def find_pos(sub_arr: [], item_index: int):
    if item_index == 0:
        return item_index

    correct_pos = item_index
    for pos in range(item_index - 1, 0, -1):
        if sub_arr[pos] > sub_arr[item_index]:
            correct_pos = pos
        else:
            break
    return correct_pos


def test_flip_sort():
    arr = [1, 9, 3, 5]
    for i in range(0, 4):
        correct_pos = find_pos(arr, i)
        flip_sort(arr, correct_pos, i)

    print(arr)


def test_find_pos():
    a = [1, 9, 3, 5]

    for i in range(0, 4):
        print("%d: %d --> %d" % (i, a[i], find_pos(a, i)))

if __name__ == '__main__':
    #main()
    #test_find_pos()
    test_flip_sort()
    #a = [1, 9, 3, 5]
    #flip_sort(a, 1, 2)
    #print(a)