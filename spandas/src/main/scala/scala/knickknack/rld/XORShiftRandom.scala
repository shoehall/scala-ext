package scala.knickknack.rld

import java.nio.ByteBuffer
import java.util.Random
import scala.util.hashing.MurmurHash3

class XORShiftRandom(init: Long) extends Random(init) {

  def this() = this(System.nanoTime)

  private var seed = XORShiftRandom.hashSeed(init)

  // we need to just override next - this will be called by nextInt, nextDouble,
  // nextGaussian, nextLong, etc.
  override protected def next(bits: Int): Int = {
    var nextSeed = seed ^ (seed << 21)
    nextSeed ^= (nextSeed >>> 35)
    nextSeed ^= (nextSeed << 4)
    seed = nextSeed
    (nextSeed & ((1L << bits) -1)).asInstanceOf[Int]
  }

  override def setSeed(s: Long) {
    seed = XORShiftRandom.hashSeed(s)
  }
}


object XORShiftRandom {
  /** Hash seeds to have 0/1 bits throughout. */
  def hashSeed(seed: Long): Long = {
    val bytes = ByteBuffer.allocate(java.lang.Long.SIZE).putLong(seed).array()
    val lowBits = MurmurHash3.bytesHash(bytes, MurmurHash3.arraySeed)
    val highBits = MurmurHash3.bytesHash(bytes, lowBits)
    (highBits.toLong << 32) | (lowBits.toLong & 0xFFFFFFFFL)
  }
}
