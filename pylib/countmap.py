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
