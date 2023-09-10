---
sidebar_position: 200
title: xls2yml
---


## Synopsis

**starlake xls2yml [options]**

## Description


## Parameters

Parameter|Cardinality|Description
---|---|---
--files:`<value>`|*Required*|List of Excel files describing domains & schemas or jobs
--encryption:`<value>`|*Optional*|If true generate pre and post encryption YML
--iamPolicyTagsFile:`<value>`|*Optional*|If true generate IAM PolicyTags YML
--delimiter:`<value>`|*Optional*|CSV delimiter to use in post-encrypt YML.
--privacy:`<value>`|*Optional*|What privacy policies should be applied in the pre-encryption phase ? All privacy policies are applied by default.
--outputPath:`<value>`|*Optional*|Path for saving the resulting YAML file(s). Comet domains path is used by default.
--policyFile:`<value>`|*Optional*|Optional File for centralising ACL & RLS definition.
--job:`<value>`|*Optional*|If true generate YML for a Job.
