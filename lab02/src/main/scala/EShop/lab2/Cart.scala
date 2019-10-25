package EShop.lab2

case class Cart(items: Seq[Any]) {
  def contains(item: Any): Boolean = false
  def addItem(item: Any): Cart     = null
  def removeItem(item: Any): Cart  = null
  def size: Int                    = 1
}

object Cart {
  def empty: Cart = null
}
