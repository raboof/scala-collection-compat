/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package scala.collection.compat

import scala.reflect.ClassTag
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder
import scala.collection.{immutable => i, mutable => m}

/* builder optimized for a single ++= call, which returns identity on result if possible
 * and defers to the underlying builder if not.
 */
private final class IdentityPreservingBuilder[A, CC[X] <: TraversableOnce[X]](that: Builder[A, CC[A]])(implicit ct: ClassTag[CC[A]])
    extends Builder[A, CC[A]] {
  var collection: CC[A] = null.asInstanceOf[CC[A]]
  var ruined = false

  final override def ++=(elems: TraversableOnce[A]): this.type =
      elems match {
        case ct(ca) if (collection == null && !ruined) =>  {
          collection = ca
          this
        }
        case _ => {
          ruined = true
          if (collection != null) that ++= collection
          that ++= elems
          collection = null.asInstanceOf[CC[A]]
          this
        }
      }

  final def +=(elem: A): this.type = {
    collection = null.asInstanceOf[CC[A]]
    ruined = true
    that += elem
    this
  }
  final def clear(): Unit = {
    collection = null.asInstanceOf[CC[A]]
    if (ruined) that.clear()
  }
  final def result(): CC[A] = if(ruined || (collection == null)) that.result() else collection
}

private[compat] object CompatImpl {
  def simpleCBF[A, C](f: => Builder[A, C]): CanBuildFrom[Any, A, C] = new CanBuildFrom[Any, A, C] {
    def apply(from: Any): Builder[A, C] = apply()
    def apply(): Builder[A, C]          = f
  }

  type ImmutableBitSetCC[X] = ({ type L[_] = i.BitSet })#L[X]
  type MutableBitSetCC[X]   = ({ type L[_] = m.BitSet })#L[X]
}
