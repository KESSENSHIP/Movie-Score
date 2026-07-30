import pymysql
conn = pymysql.connect(host='localhost', port=3306, user='root',
                       password='M20054921', database='movie_db', charset='utf8mb4')
with conn.cursor() as cur:
    cur.execute('SELECT COUNT(*) FROM movie_prediction')
    print(f'movie_prediction 表总记录数: {cur.fetchone()[0]}')
    cur.execute('SELECT MIN(predicted_score), MAX(predicted_score), ROUND(AVG(predicted_score),2) FROM movie_prediction')
    row = cur.fetchone()
    print(f'预测评分范围: {row[0]} - {row[1]}  平均分: {row[2]}')
    cur.execute("SELECT stat_key, stat_value FROM movie_stats WHERE stat_type='rating_prediction' ORDER BY id DESC LIMIT 6")
    print('\nmovie_stats 评估指标:')
    for r in cur.fetchall():
        print(f'  {r[0]}: {r[1]}')
conn.close()
