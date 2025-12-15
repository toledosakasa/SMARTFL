project_bug_nums = {"Lang": 65, "Math": 106,
                    "Time": 27, "Closure": 176, "Chart": 26}

def utf8open(filename):
    return open(filename, encoding='utf-8', errors='ignore')

def utf8open_w(filename):
    return open(filename, 'w+', encoding='utf-8', errors='ignore')

def utf8open_a(filename):
    return open(filename, 'a+', encoding='utf-8', errors='ignore')


def decorrected(proj:str):
    path = f"./metadata_cached/{proj}"
    for i in range(project_bug_nums[proj]):
        id = i + 1
        try:
            logfile = utf8open(f'{path}/{id}.log')
        except IOError:
            print(f'no {proj} {id} log')
            continue
        lines = logfile.readlines()
        logfile.close()
        try:
            outfile = utf8open_w(f'{path}/{id}.log')
        except IOError:
            continue
        for line in lines:
            if line.strip("\n") != "corrected=true":
                outfile.write(line)

if __name__ == '__main__':
    for proj in ('Lang', 'Time', 'Chart', 'Math'):
        decorrected(proj)
    print('done')