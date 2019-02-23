#!/usr/bin/env python3

import re
import sys

class DocumentSearch:

    def __init__(self, file: str):
        self.word_postling = self.parseDocPostling(file)

    def parseDocPostling(self, file: str):
        with open(file) as f:
            read_data = f.read()
        rawWords = read_data.lower().split()

        p = re.compile('\W*([a-z]+)\W*')
        formated_words = []
        for rawWord in rawWords:
            m = p.match(rawWord)
            if m:
                formatedWord = m.group(1)
                formated_words.append(formatedWord)
                #print("%s %s" % (rawWord, formatedWord))
            else:
                print("%s" % rawWord)

        vocabulary: {str: []} = {}
        for i in range(0, len(formated_words)):
            word = formated_words[i]
            postling = vocabulary.get(word, [])
            postling.append(i)
            vocabulary.setdefault(word, postling)

        return vocabulary

    def searchPhrase(self, phrase: str):
        token_arr = phrase.split()
        upper = -1
        for token in token_arr:
            t_postling = self.word_postling.get(token, None)
            if t_postling is None:
                upper = -1
                break
            else:
                upper = DocumentSearch.findNext(t_postling, upper)
                if upper is None:
                    upper = -1
                    break


        if upper == -1:
            print("Not found phrase")
            return None, None

        v = upper + 1
        reversed_token_arr = list(reversed(token_arr))
        for token in reversed_token_arr:
            t_postling = self.word_postling.get(token, None)
            v = DocumentSearch.findPre(t_postling, v)

        if (upper - v + 1) == len(token_arr):
            return v, upper
        else:
            print("Not found phrase")
            return None, None


    @staticmethod
    def findNext(pos_arr: [], start: int):
        pos = None
        for i in pos_arr:
            if i > start:
                pos = i
                break
        return pos


    @staticmethod
    def findPre(pos_arr: [], start: int):
        pos = None
        for i in range(len(pos_arr) - 1, -1, -1):
            if pos_arr[i] < start:
                pos = pos_arr[i]
                break
        return pos

file = "/Users/yangqingfeng/workspace/yqf-workspace/demo-code/python3/data/search_article.txt"
def testParsingPostling():
    docSearch = DocumentSearch(file)
    print(docSearch.word_postling)

def testSearchNext():
    pos_arr = [0, 20, 40, 44, 48, 55, 58, 64, 69]
    start_arr = [-1, 3, 20, 45, 70]
    print("Find Next")
    for startPos in start_arr:
        ret = DocumentSearch.findNext(pos_arr, startPos)
        print("Next: start=%s, pos=%s" % (startPos, ret))

    print("Find Pre")
    for startPos in start_arr:
        ret = DocumentSearch.findPre(pos_arr, startPos)
        print("Pre: start=%s, pos=%s" % (startPos, ret))

def testSearchPhrase():
    phrase_arr = ["hard part", "at once", "into the block", "into block","at all"]
    #phrase_arr = ["into block"]
    for phrase in phrase_arr:
        docSearch = DocumentSearch(file)
        v, u = docSearch.searchPhrase(phrase)
        print("phrase:[%s], pos=[%s, %s]" % (phrase, v, u))

if __name__ == "__main__":
    testSearchPhrase()