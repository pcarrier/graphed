import com.pcarrier.graphed.graphql.Parser
import com.pcarrier.graphed.graphql.ToSexp

fun graphqlToSexp(str: String): String {
    val doc = Parser(str).parse()
    return ToSexp.document(doc).toString()
}
