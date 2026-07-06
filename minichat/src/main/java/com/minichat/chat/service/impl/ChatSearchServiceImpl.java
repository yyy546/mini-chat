package com.minichat.chat.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.minichat.chat.entity.EsChatMessage;
import com.minichat.chat.service.ChatSearchService;
import com.minichat.common.core.constants.MessageConstants;
import com.minichat.common.core.constants.SessionConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSearchServiceImpl implements ChatSearchService {

    private final ElasticsearchClient esClient;

    public void initIndex(){
        try{
            boolean exists = esClient.indices().exists(req -> req.index(MessageConstants.ES_CHAT_MESSAGE_INDEX)).value();
            if(!exists){
                createIndex();
            }else{
                log.info("索引已存在，无需创建");
            }
            // 预热连接
            esClient.search(s->s.index(MessageConstants.ES_CHAT_MESSAGE_INDEX).size(0),EsChatMessage.class);
            log.info("Elasticsearch 连接预热完成");
        } catch (IOException e) {
            log.error("初始化索引失败",e);
        }
    }

    private void createIndex() {
        try {
            esClient.indices().create(c -> c
                    .index(MessageConstants.ES_CHAT_MESSAGE_INDEX)
                    .settings(s -> s
                            .analysis(a -> a
                                    .tokenizer("my_ngram_tokenizer", t -> t
                                            .definition(d -> d // 使用 definition 包装
                                                    .ngram(n -> n
                                                            .minGram(1)
                                                            .maxGram(2)
                                                            .tokenChars(new ArrayList<>())
                                                    )
                                            )
                                    )
                                    .analyzer("custom_ik_standard_analyzer", ana -> ana
                                            .custom(cust -> cust
                                                    .tokenizer("ik_max_word")
                                                    .filter("lowercase")
                                            )
                                    )
                                    .analyzer("my_ngram_analyzer", ana -> ana
                                            .custom(cust -> cust
                                                    .tokenizer("my_ngram_tokenizer")
                                                    .filter("lowercase")
                                            )
                                    )
                            )
                    )
                    .mappings(m -> m
                            .properties("id", p -> p.keyword(k -> k))
                            .properties("dbId", p -> p.long_(l -> l))
                            .properties("type", p -> p.integer(i -> i))
                            .properties("senderId", p -> p.long_(l -> l))
                            .properties("senderNickName", p -> p.keyword(k -> k))
                            .properties("targetId", p -> p.long_(l -> l))
                            .properties("content", p -> p.text(t -> t
                                            .analyzer("custom_ik_standard_analyzer")
                                            .searchAnalyzer("ik_smart")
                                            .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(32000)))
                                            .fields("ngram", f -> f.text(nt -> nt
                                                    .analyzer("my_ngram_analyzer")
                                                    .searchAnalyzer("my_ngram_analyzer")
                                            ))
                                    )
                            )
                            .properties("messageType", p -> p.integer(i -> i))
                            .properties("fileName", p -> p.text(t -> t
                                            .analyzer("custom_ik_standard_analyzer")
                                            .searchAnalyzer("ik_smart")
                                            .fields("keyword", f -> f.keyword(k -> k.ignoreAbove(32000)))
                                            .fields("ngram", f -> f.text(nt -> nt
                                                    .analyzer("my_ngram_analyzer")
                                                    .searchAnalyzer("my_ngram_analyzer")
                                            ))
                                    )
                            )
                            .properties("fileUrl", p -> p.keyword(k -> k))
                            .properties("sendTime", p -> p.date(d -> d))
                    )
            );
            log.info("创建索引成功");
        } catch (IOException e) {
            log.error("创建索引失败", e);
        }
    }

    @Override
    public List<EsChatMessage> search(String keyword, Integer type, Long targetId, Long currentUserId) {
        try {
            //构建bool查询
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();
            // 关键词匹配逻辑：同时支持分词匹配和通配符匹配
            boolQuery.must(m -> m
                    .bool(b -> b
                            .should(s -> s
                                    .multiMatch(mu -> mu
                                            .query(keyword)
                                            .fields("content", "fileName")
                                    )
                            )
                            .should(s -> s
                                    .match(m1 -> m1
                                            .field("content.ngram")
                                            .query(keyword)
                                    )
                            )
                            .should(s -> s
                                    .match(m2 -> m2
                                            .field("fileName.ngram")
                                            .query(keyword)
                                    )
                            )
                    )
            );
            // 会话类型必须匹配
            boolQuery.filter(f -> f
                    .term(t -> t
                            .field("type")
                            .value(type)
                    )
            );

            if (SessionConstants.SESSION_TYPE_GROUP.equals(type)) {
                boolQuery.filter(f -> f
                        .term(t -> t
                                .field("targetId")
                                .value(targetId)
                        )
                );
            } else {
                boolQuery.filter(f -> f
                        .bool(b -> b
                                .should(s -> s
                                        .term(t -> t
                                                .field("senderId")
                                                .value(currentUserId)
                                        )
                                )
                                .should(s -> s
                                        .term(t -> t
                                                .field("targetId")
                                                .value(currentUserId)
                                        )
                                )
                        )
                );

                boolQuery.filter(f -> f
                        .bool(b -> b
                                .should(s -> s
                                        .term(t -> t
                                                .field("senderId")
                                                .value(targetId)
                                        )
                                )
                                .should(s -> s
                                        .term(t -> t
                                                .field("targetId")
                                                .value(targetId)
                                        )
                                )
                        )
                );
            }

            SearchResponse<EsChatMessage> response = esClient.search(s -> s
                            .index(MessageConstants.ES_CHAT_MESSAGE_INDEX)
                            .query(q -> q
                                    .bool(boolQuery.build()
                                    )
                            )
                            .highlight(h -> h
                                    .fields("content", f -> f
                                            .preTags("<em style='color:red'>")
                                            .postTags("</em>")
                                    ).fields("fileName", f -> f
                                            .preTags("<em style='color:red'>")
                                            .postTags("</em>")
                                    )
                            )
                    , EsChatMessage.class);

            List<EsChatMessage> result = new ArrayList<>();
            response.hits().hits().forEach(hit -> {
                EsChatMessage message = hit.source();
                if(hit.highlight().get("content") != null){
                    message.setContent(hit.highlight().get("content").get(0));
                }
                if(hit.highlight().get("fileName") != null){
                    message.setFileName(hit.highlight().get("fileName").get(0));
                }
                result.add(message);
            });

            return result;
        } catch (Exception e) {
            log.error("搜索聊天记录失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void saveChatMessage(EsChatMessage message) {
        try{
            String esId = ((SessionConstants.SESSION_TYPE_PRIVATE.equals(message.getType())) ?
                    MessageConstants.ES_CHAT_MESSAGE_TYPE_PRIVATE : MessageConstants.ES_CHAT_MESSAGE_TYPE_GROUP) + message.getDbId();
            message.setId(esId);

            esClient.index(i->i
                    .index(MessageConstants.ES_CHAT_MESSAGE_INDEX)
                    .id(esId)
                    .document(message));
            log.info("保存聊天记录成功, esId: {}", esId);
        }catch (Exception e){
            log.error("保存聊天记录失败", e);
        }
    }

    @Override
    public void deleteChatMessage(String id) {
        try{
            esClient.delete(d->d
                    .index(MessageConstants.ES_CHAT_MESSAGE_INDEX)
                    .id(id)
            );
            log.info("删除聊天记录成功, esId: {}", id);
        }catch (Exception e){
            log.error("删除聊天记录失败", e);
        }
    }

}
