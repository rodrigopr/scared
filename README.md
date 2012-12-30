ScaRed
=========

ScaRed is a minimal Object-KeyValue Mapping library for Redis.

All it needs to work is a annotation on the class, as the following example:

```scala
@BeanInfo
@Persist(name = "server",
  customIndexes = Array(
    new Index(fields = Array("enviroment")),
    new Index(fields = Array("roles"))
  )
)
case class Server(id: Long, name: String, ip: String, enviroment: String, roles: List[String]) {
  def this() = this(0, null, null, null, null, null, null)
}
```

Note the empty-param constructor and the @BeanInfo, those are required by the serialization([sjson](https://github.com/debasishg/sjson/wiki/Reflection-based-JSON-Serialization)), it will changed as soon as possible.
Also, is really easy to change the SerDe, look at `src/example/custom_serializer.scala`.

Usage
----------

#### Save
```scala
val model = Server(100l, "server1", "127.0.0.1", "dev", List("redis-srv", "hbase")
context.save(model)
```

#### Update
```scala
val updated: Option[Server] = context.update[Server](100l, m => m.copy(name = "server02", enviroment="production"))
```

#### Delete
```scala
context.delete[Server](100l)
```

#### Load
```scala
val server: Option[Server] = context.load[Server](100l)
```

#### Querying(Simple)
```scala
val servers: Iterator[Server] = context.query[Server](new Where{ 'enviroment === "dev" }).execute()
```

#### Querying(Paginating)
```scala
val servers: Iterator[Server] = context
    .query[Server](new Where{ 'enviroment === "dev" })
    .paginate(FixedInterval(0, 10))
    .execute()
```
* Can also paginate through score(look at ScorePagination)

#### Querying(Join)
```scala
val servers: Iterator[Server] = context
    .query[Server](new Where{ 'enviroment === "dev" })
    .and(new Where{ 'role = "hbase" })
    .joinMethod(JoinMethod.INTERSECTION)
    .execute()
```

For more examples look at `src/example/` and `src/test/scala` folders.

TODO
-----------
  - Reduce boilerplate required(maybe use with classes without annotation?)
  - Better default serialization
  - Auto-Increment support
  - Support auto Reference/Relationship
  - Support inherited fields
  - Better configuration (only localhost:6379 right now)
  - Support sharding(index join would be disable)
  - Better error handling(using only RuntimeExceltion right now)


License
-----------
This software is licensed under the Apache 2 license, quoted below.

Licensed under the Apache License, Version 2.0 (the ?License?); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an ?AS IS? BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.