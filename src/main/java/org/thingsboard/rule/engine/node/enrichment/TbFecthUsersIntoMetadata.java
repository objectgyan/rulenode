/**
 * Copyright © 2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.node.enrichment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.data.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;
import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import org.thingsboard.server.common.data.id.*;
import java.util.UUID;
import java.util.*;
import java.lang.StringBuilder;
import java.security.KeyStore.Entry.Attribute;

/**
 * Created by mshvayka on 10.08.18.
 */
@RuleNode(type = ComponentType.ENRICHMENT, name = "im user group", configClazz = TbFecthUsersIntoMetadataConfiguration.class, nodeDescription = "Fetch the list of users from a user group", nodeDetails = "If groupId is passed in the message payload with the <code>groupId</code>, Email address and phone number is added to metadata.", uiResources = {
		"static/rulenode/custom-nodes-config.js" }, configDirective = "tbEnrichmentNodeFecthUsersIntoMetadataConfig")
public class TbFecthUsersIntoMetadata implements TbNode {

	private static final ObjectMapper mapper = new ObjectMapper();

	private TbFecthUsersIntoMetadataConfiguration config;
	private String inputKey;
	private String outputKey;

	@Override
	public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
		this.config = TbNodeUtils.convert(configuration, TbFecthUsersIntoMetadataConfiguration.class);
		inputKey = config.getInputKey();
		outputKey = config.getOutputKey();
	}

	@Override
	public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
		String groupId = msg.getMetaData().getValue("ss_userGroup");
		EntityGroupId entityGroupId = new EntityGroupId(UUID.fromString(groupId));
		
		TimePageData<ShortEntityView> value = ctx.getUserService()
				.findUsersByEntityGroupId(ctx.getTenantId(), entityGroupId, new TimePageLink(1000)).get();
		
		List<ShortEntityView> users = value.getData();

		String emailAddresses = "";
		String phoneNumbers = "";

		for (int i = 0; i < users.size(); i++) {
			ShortEntityView user = users.get(i);
			emailAddresses += user.getName() + ",";
			Optional<AttributeKvEntry> phoneAttribute = ctx.getAttributesService()
					.find(ctx.getTenantId(), user.getId(), "SERVER_SCOPE", "phoneNumber").get();
			if(phoneAttribute.get() != null && phoneAttribute.get().getStrValue() != null) {
				phoneNumbers+=phoneAttribute.get().getStrValue().get() +",";
			}
		}

		msg.getMetaData().putValue("emailAddresses", emailAddresses);
		msg.getMetaData().putValue("phoneNumbers", phoneNumbers);
		msg = ctx.newMsg(msg.getType(), msg.getOriginator(), msg.getMetaData(), msg.getData());
		ctx.tellNext(msg, SUCCESS);
	}

	@Override
	public void destroy() {

	}
}
