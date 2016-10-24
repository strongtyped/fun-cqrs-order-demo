api.put() {
  http PUT http://localhost:9000/$1 --verbose < last.json
}

api.post() {
  http POST http://localhost:9000/$1 --verbose < last.json
}

api.delete() {
  http DELETE http://localhost:9000/$1 --verbose
}

api.get() {
  http GET http://localhost:9000/$1 --verbose
}


order.create() {
  cat << EOF > last.json
{}
EOF
    api.put order
}



order.get() {
  api.get order/$1
}


list.payed.orders() {
  api.get orders/payed
}

list.cancelled.orders() {
  api.get orders/cancelled
}

list.open.orders() {
  api.get orders/open
}



order.addItem() {
cat << EOF > last.json
{
  "value": "$2"
}
EOF

  api.post order/$1/items
}

order.removeItem() {
  api.delete order/$1/items/$2
}

order.cancel() {
  api.delete order/$1
}


order.pay() {
cat << EOF > last.json
{
  "value": "$2"
}
EOF

  api.post order/$1
}