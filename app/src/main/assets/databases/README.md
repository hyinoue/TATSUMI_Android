このディレクトリに、別で作成した `tatsumiDB_handy.sqlite` を配置してください。
起動時に `Room.createFromAsset("databases/tatsumiDB_handy.sqlite")` で読み込みます。

パワーシェルでこのパスに移動 → 以下コマンドでreset_schema.sql内の処理を実行
sqlite3 .\tatsumiDB_handy.sqlite ".read .\reset_schema.sql"
