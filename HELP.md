# 更新履歴

- [Markdown cheat](https://gist.github.com/mignonstyle/083c9e1651d7734f84c99b8cf49d57fa)
- //TODO の項目は書きかけ、未定です

### 2019/07/06 宮下

- CategoryServiceを追加・修正
- TODO ユーザーに表示するエラーの処理をどうするか。
	- チェック用メソッドをpublicで作成し、Controller側で処理(これでいく)
	- その他、DB制約などのエラーは例外を作成し、Throwする
