package scala.io

class User(val name: String, val age: Int, val weight: Double) {
  def ff: Boolean = false

  override def toString: String = s"User($name, $age, $weight)"

  override def equals(obj: Any): Boolean = obj match {
    case user: User =>
      name == user.name && age == user.age && weight == user.weight
    case _ =>
      false
  }
}

case class Bike(val name: String, val user: User)

