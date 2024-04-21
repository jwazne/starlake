/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  * contributor license agreements.  See the NOTICE file distributed with
 *  * this work for additional information regarding copyright ownership.
 *  * The ASF licenses this file to You under the Apache License, Version 2.0
 *  * (the "License"); you may not use this file except in compliance with
 *  * the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 *
 */

package ai.starlake.schema.model

import java.io.{InputStream, StringWriter}

import ai.starlake.TestHelper
import ai.starlake.schema.handlers.SchemaHandler
import ai.starlake.schema.model.Severity._

class SchemaSpec extends TestHelper {

  new WithSettings() {
    val schemaHandler = new SchemaHandler(storageHandler)

    "Attribute type" should "be valid" in {
      val stream: InputStream =
        getClass.getResourceAsStream("/sample/default.sl.yml")
      val lines =
        scala.io.Source.fromInputStream(stream).getLines().mkString("\n")
      val types = mapper.readValue(lines, classOf[TypesDesc])
      val attr = Attribute(
        "attr",
        "invalid-type", // should raise error non existent type
        Some(true),
        required = true,
        TransformInput(
          "MD5",
          false
        ) // Should raise an error. Privacy cannot be applied on types other than string
      )

      attr.checkValidity(schemaHandler) shouldBe Left(
        List(
          ValidationMessage(Error, "Attribute.primitiveType", "Invalid Type invalid-type")
        )
      )
    }

    "Attribute privacy" should "appliable to any type" in {
      val attr = Attribute(
        "attr",
        "long",
        Some(true),
        required = true,
        TransformInput(
          "ApproxLong(20)",
          false
        ) // Should raise an error. Privacy cannot be applied on types other than stringsettings = settings
      )
      attr.checkValidity(schemaHandler) shouldBe Right(true)
    }

    "Sub Attribute" should "be present for struct types only" in {
      val attr = Attribute(
        "attr",
        "struct",
        Some(true),
        required = true,
        TransformInput(
          "ApproxLong(20)",
          false
        ), // Should raise an error. Privacy cannot be applied on types other than string
        attributes = List[Attribute]()
      )
      val expectedErrors = List(
        ValidationMessage(
          Error,
          "Attribute.primitiveType",
          "Attribute Attribute(attr,struct,Some(true),true,ApproxLong(20),None,None,None,List(),None,None,Set()) : Struct types must have at least one attribute."
        )
      )

      attr.checkValidity(schemaHandler) shouldBe Left(expectedErrors)
    }

    "Position serialization" should "output all fields" in {
      val yml = loadTextFile(s"/expected/yml/position_serialization.sl.yml")

      val attr =
        Attribute("hello", position = Some(Position(1, 2)))
      val writer = new StringWriter()
      mapper.writer().writeValue(writer, attr)
      logger.info("--" + writer.toString + "--")
      logger.info("++" + yml + "++")
      writer.toString.trim should equal(yml)
    }

    "Default value for an attribute" should "only be used for non obligatory fields" in {
      val requiredAttribute =
        Attribute("requiredAttribute", "long", required = true, default = Some("10"))
      requiredAttribute.checkValidity(schemaHandler) shouldBe Left(
        List(
          ValidationMessage(
            Error,
            "Attribute.default",
            s"attribute with name ${requiredAttribute.name} - default value valid for optional fields only"
          )
        )
      )

      val optionalAttribute =
        Attribute("optionalAttribute", "long", required = false, default = Some("10"))
      optionalAttribute.checkValidity(schemaHandler) shouldBe Right(true)
    }
    "Ignore attribute " should "be used only when file format is flat DSV, JSON_FLAT, POSITION" in {
      val meta = new Metadata(
        format = Some(Format.JSON),
        encoding = None,
        multiline = Some(false),
        array = Some(false),
        withHeader = Some(true),
        separator = None,
        quote = None,
        escape = None,
        sink = None,
        ignore = Some(".*")
      )
      meta
        .checkValidity(schemaHandler)
        .isInstanceOf[Left[List[ValidationMessage], Boolean]] shouldBe true
    }
    "Ignore attribute " should "on DSV should be UDF" in {
      val meta = new Metadata(
        format = Some(Format.DSV),
        encoding = None,
        multiline = Some(false),
        array = Some(false),
        withHeader = Some(true),
        separator = None,
        quote = None,
        escape = None,
        sink = None,
        ignore = Some(".*")
      )
      val res = meta.checkValidity(schemaHandler)
      meta
        .checkValidity(schemaHandler)
        .isInstanceOf[Left[List[ValidationMessage], Boolean]] shouldBe true
    }
  }
}
