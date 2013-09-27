rm -Rf gen-java gen-js
find ./thrift/ -type f | xargs -I repme thrift -gen java repme
find ./thrift/ -type f | xargs -I repme thrift -gen js:jquery repme
