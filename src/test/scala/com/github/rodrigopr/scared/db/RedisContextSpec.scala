package com.github.rodrigopr.scared.db

import org.scalatest.{BeforeAndAfterEach, FeatureSpec}
import com.redis.RedisClient
import com.github.rodrigopr.scared.testmodel.{NoIdModel, InvalidModel, Simple, Server}
import sjson.json.Serializer.{ SJSON => jsonSerializer }
import org.scalatest.matchers.ShouldMatchers
import com.github.rodrigopr.scared.serializer.SJsonSerDe

class RedisContextSpec extends FeatureSpec with BeforeAndAfterEach with ShouldMatchers {
  val redis = new RedisClient
  val context = new RedisContext(new SJsonSerDe)

  // Helpers
  val firstServerOnly = Some(List("100"))
  val secondServerOnly = Some(List("200"))
  val bothServer = Some(List("100", "200"))
  val noServer = Some(List())

  override protected def beforeEach() {
    super.beforeEach()
    redis.flushall
  }

  feature("Store object in redis") {
    scenario("Saving Object") {
      val model = Simple(1000l, "MySimpleModel")

      context.save(model)

      redis.get("simple:1000") should be( Some(new String(jsonSerializer.out(model))) )
    }

    scenario("Updating Object using save") {
      val model = Simple(1000l, "MySimpleModel")

      context.save(model)

      val modelUpdate = Simple(1000l, "MySimpleModel-Update")

      context.save(modelUpdate)

      redis.get("simple:1000") should be( Some(new String(jsonSerializer.out(modelUpdate))) )
    }

    scenario("Updating Object") {
      val model = Simple(1000l, "MySimpleModel")

      context.save(model)

      val modelUpdate = context.update[Simple](1000l, m => m.copy(name = "MySimpleModel-Update")).get

      modelUpdate should be(Simple(1000l, "MySimpleModel-Update"))

      redis.get("simple:1000") should be( Some(new String(jsonSerializer.out(modelUpdate))) )
    }

    scenario("Updating non existing Object") {
      val modelUpdated = context.update[Simple](1000l, m => m.copy(name = "MySimpleModel-Update"))

      modelUpdated should be(None)
    }

    scenario("Loading saved object") {
      val model = Simple(1000l, "MySimpleModel")

      context.save(model)
      val loaded = context.load[Simple](1000l)

      loaded should be(Some(model))
    }

    scenario("Loading no existing object") {
      val loaded = context.load[Simple](1000l)

      loaded should be(None)
    }

    scenario("Deleting object using id") {
      val model = Simple(1000l, "MySimpleModel")

      context.save(model)
      context.delete[Simple](1000l)

      val loaded = context.load[Simple](1000l)

      loaded should be(None)
    }

    scenario("Deleting object using model") {
      val model = Simple(1000l, "MySimpleModel")

      context.save(model)
      context.deleteModel(model)

      val loaded = context.load[Simple](1000l)

      loaded should be(None)
    }

    scenario("Save creates indexes") {
      val model = Server(100l, "name", "i", "f", "g", "e", List("r1", "r2"))
      val model2 = Server(200l, "name2", "i", "f", "g", "e", List("r1", "r3"))

      context.save(model)
      context.save(model2)

      redis.zrange("@server[name]=>name", 0, -1) should be (firstServerOnly)

      redis.zrange("@server[roles]=>r1", 0, -1) should be (bothServer)
      redis.zrange("@server[roles]=>r2", 0, -1) should be (firstServerOnly)
      redis.zrange("@server[roles]=>r3", 0, -1) should be (secondServerOnly)
    }

    scenario("Updating indexes") {
      val model = Server(100l, "name", "i", "f", "g", "e", List("r1", "r2"))

      val model2 = Server(200l, "name2", "i", "f", "g", "e", List("r1", "r3"))
      context.save(model)
      context.save(model2)

      redis.zrange("@server[roles]=>r1", 0, -1) should be (bothServer)
      redis.zrange("@server[roles]=>r2", 0, -1) should be (firstServerOnly)
      redis.zrange("@server[roles]=>r3", 0, -1) should be (secondServerOnly)

      context.save(model.copy(name = "name-updated", roles = List("r3", "r2")))

      redis.zrange("@server[name]=>name", 0, -1) should be (noServer)
      redis.zrange("@server[name]=>name-updated", 0, -1) should be (firstServerOnly)

      redis.zrange("@server[roles]=>r1", 0, -1) should be (secondServerOnly)
      redis.zrange("@server[roles]=>r2", 0, -1) should be (firstServerOnly)
      redis.zrange("@server[roles]=>r3", 0, -1) should be (bothServer)
    }

    scenario("Saving composite indexes") {
      val model = Server(100l, "name", "i", "f", "grp", "env", List("r1", "r2"))

      context.save(model)

      redis.zrange("@server[enviroment:group]=>env:$:grp", 0, -1) should be (firstServerOnly)

      context.save(model.copy(enviroment = "env-updated"))

      redis.zrange("@server[enviroment:group]=>env:$:grp", 0, -1) should be (noServer)
      redis.zrange("@server[enviroment:group]=>env-updated:$:grp", 0, -1) should be (firstServerOnly)

      context.save(model.copy(enviroment = "env-updated", group = "grp-updated"))

      redis.zrange("@server[enviroment:group]=>env:$:grp", 0, -1) should be (noServer)
      redis.zrange("@server[enviroment:group]=>env-updated:$:grp", 0, -1) should be (noServer)
      redis.zrange("@server[enviroment:group]=>env-updated:$:grp-updated", 0, -1) should be (firstServerOnly)
    }

    scenario("Using autoincrement for id")(pending)

    scenario("Using incorrect model") {
      val model = InvalidModel(100l, "invalid")

      val exception = intercept[RuntimeException] {
        context.save(model)
      }

      exception.getMessage should be("Model ain't mapped with @Persist annotation")
    }

    scenario("Using model without @id") {
      val model = NoIdModel(100l, "invalid")

      val exception = intercept[RuntimeException] {
        context.save(model)
      }

      exception.getMessage should be("requirement failed: Type noid needs exactly one @id attribute")
    }
  }
}
