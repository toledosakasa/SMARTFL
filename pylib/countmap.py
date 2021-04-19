class CountMap:

    def __init__(self):
        self.relevant_cnt = {}
        self.class_relevant_cnt = {}
        self.class_method_cnt = {}

    def add(self, curclass, curmethod):
        if curclass in self.relevant_cnt:
            self.class_relevant_cnt[curclass] += 1
            methodmap = self.relevant_cnt[curclass]
            if curmethod in methodmap:
                methodmap[curmethod] += 1
            else:
                methodmap[curmethod] = 1
                self.class_method_cnt[curclass] += 1
        else:
            self.relevant_cnt[curclass] = {curmethod: 1}
            self.class_method_cnt[curclass] = 1
            self.class_relevant_cnt[curclass] = 1

    def method_filter(self, triggertests, testmethods):
        max_method_count = 50
        ret = []
        # always keep trigger tests.
        for triggerclass in triggertests:
            for triggermethod in triggertests[triggerclass]:
                ret.append((triggerclass, triggermethod))
        if len(self.class_relevant_cnt) == 0:
            # failed to trace trigger. use all tests.
            for testclass in testmethods:
                if testclass in triggertests:
                    for testmethod in testmethods[testclass]:
                        ret.append((testclass, testmethod))
        else:
            # filter by relevance.
            collected = []
            for classname in self.relevant_cnt:
                for methodname in self.relevant_cnt[classname]:
                    collected.append((classname, methodname))
            sort_collected = sorted(
                collected, key=lambda x: self.relevant_cnt[x[0]][x[1]], reverse=True)
            ret = (ret + sort_collected)[:max_method_count]
        ret = list(set(ret))
        # i = 0
        # sum = 0
        # debugp = []
        # for r in ret:
        #     sum = sum + self.relevant_cnt[r[0]][r[1]]
        #     debugp.append(self.relevant_cnt[r[0]][r[1]])
        #     i = i + 1
        # print('avg coverage: ', sum / i)
        # print(debugp)

        return ret

    def filter(self, triggertests):
        max_class_count = 5
        sorted_classes = sorted(self.class_relevant_cnt.keys(
        ), key=lambda t: self.class_relevant_cnt[t]/self.class_method_cnt[t], reverse=True)
        sorted_classes_trim = sorted_classes[:max_class_count]

        # always keep trigger test classes.
        for triggerclass in triggertests:
            if (not triggerclass in sorted_classes_trim) and (triggerclass in sorted_classes):
                sorted_classes_trim.append(triggerclass)

        # get methods
        max_method_count = 10
        ret = []
        for curclass in sorted_classes_trim:
            curmap = self.relevant_cnt[curclass]
            sorted_methods = sorted(
                curmap.keys(), key=lambda t: curmap[t], reverse=True)
            sorted_methods = sorted_methods[:max_method_count]
            for curmethod in sorted_methods:
                ret.append((curclass, curmethod))
        return ret
