lines = sc.textFile("hdfs://localhost:9000/user/karltrout/HADDS/*/*/*/*/nas_*_flat.txt")
rows = lines.map(lambda line: line.split(" "))
types = rows.map(lambda r: str(r[5]) if len(r) >4 else "TYP:NA")
msgCnt = types.map(lambda mt: (mt.split(':')[1], 1)).reduceByKey(lambda a,b: a+b).collect()
msgCnt
