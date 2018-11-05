package khipu.validators

import java.util.Arrays
import khipu.crypto
import khipu.rlp
import khipu.rlp.RLPImplicits._
import khipu.store.datasource.EphemDataSource
import khipu.store.trienode.ArchiveNodeStorage
import khipu.store.trienode.NodeStorage
import khipu.trie.ByteArraySerializable
import khipu.trie.MerklePatriciaTrie

object MptListValidator {

  lazy val intByteArraySerializable = new ByteArraySerializable[Int] {
    override def fromBytes(bytes: Array[Byte]): Int = rlp.decode[Int](bytes)
    override def toBytes(input: Int): Array[Byte] = rlp.encode(input)
  }

  /**
   * This function validates if a lists matches a Mpt Hash. To do so it inserts into an ephemeral MPT
   * (itemIndex, item) tuples and validates the resulting hash
   *
   * @param hash Hash to expect
   * @param toValidate Items to validate and should match the hash
   * @param vSerializable [[khipu.trie.ByteArraySerializable]] to encode Items
   * @tparam K Type of the items cointained within the Sequence
   * @return true if hash matches trie hash, false otherwise
   */
  def isValid[K](hash: Array[Byte], toValidates: Seq[K], vSerializable: ByteArraySerializable[K]): Boolean = {
    val trie = MerklePatriciaTrie[Int, K](
      source = new ArchiveNodeStorage(new NodeStorage(EphemDataSource()))
    )(intByteArraySerializable, vSerializable)

    val (_, updatedTrie) = toValidates.foldLeft((0, trie)) {
      case ((i, trie), k) =>
        (i + 1, trie.put(i, k))
    }
    val trieRoot = updatedTrie.rootHash

    Arrays.equals(hash, trieRoot)
  }
}
