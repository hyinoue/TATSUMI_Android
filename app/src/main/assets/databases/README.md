このディレクトリに、別で作成した `tatsumiDB_handy.sqlite` を配置してください。
起動時に `Room.createFromAsset("databases/tatsumiDB_handy.sqlite")` で読み込みます。

・tatsumiDB_handy.sqliteの作成（更新）方法
パワーシェルでこのパス(README.mdが置いてある場所)に移動 → 以下コマンドでreset_schema.sql内の処理を実行（reset_schema.sql内の内容でDBが作成される）
sqlite3 .\tatsumiDB_handy.sqlite ".read .\reset_schema.sql"
