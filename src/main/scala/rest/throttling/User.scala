package rest.throttling

object User {
  def apply(name: String, rps: Int) = AuthUser(name, rps)
  def apply(rps: Int) = GuestUser(rps)
}

sealed trait User {
  val name: String
  val rps: Int
}

case class AuthUser(name: String, rps: Int) extends User

case class GuestUser(rps: Int) extends User {
  val name = "guest"
}



