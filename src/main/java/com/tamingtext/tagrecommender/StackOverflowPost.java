/*
 * Copyright 2008-2011 Grant Ingersoll, Thomas Morton and Drew Farris
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * -------------------
 * To purchase or learn more about Taming Text, by Grant Ingersoll, Thomas Morton and Drew Farris, visit
 * http://www.manning.com/ingersoll
 */

/**
 * 
 */
package com.tamingtext.tagrecommender;

import java.util.Collection;

public class StackOverflowPost {
  private int id;
  private String title;
  private String body;
  private Collection<String> tags;
  private String creationDate;
  private int parentId;
  private int postTypeId;
  private int acceptedAnswerId;
  private int ownerUserId;
  private int score;
  private int viewCount;
  private int answerCount;
  private int commentCount;
  private int favoriteCount;
  
  public StackOverflowPost() {
    reInit();
  }
  
  public void reInit() {
    id = 0;
    title = null;
    body  = null;
    tags  = null;
    creationDate = null;
    parentId = 0;
    postTypeId = 0;
    acceptedAnswerId = 0;
    ownerUserId = 0;
    score = 0;
    viewCount = 0;
    answerCount = 0;
    commentCount = 0;
    favoriteCount = 0;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public Collection<String> getTags() {
    return tags;
  }

  public void setTags(Collection<String> tags) {
    this.tags = tags;
  }

  public String getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(String creationDate) {
    this.creationDate = creationDate;
  }

  public int getParentId() {
    return parentId;
  }

  public void setParentId(int parentId) {
    this.parentId = parentId;
  }

  public int getPostTypeId() {
    return postTypeId;
  }

  public void setPostTypeId(int postTypeId) {
    this.postTypeId = postTypeId;
  }

  public int getAcceptedAnswerId() {
    return acceptedAnswerId;
  }

  public void setAcceptedAnswerId(int acceptedAnswerId) {
    this.acceptedAnswerId = acceptedAnswerId;
  }

  public int getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(int ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }

  public int getViewCount() {
    return viewCount;
  }

  public void setViewCount(int viewCount) {
    this.viewCount = viewCount;
  }

  public int getAnswerCount() {
    return answerCount;
  }

  public void setAnswerCount(int answerCount) {
    this.answerCount = answerCount;
  }

  public int getCommentCount() {
    return commentCount;
  }

  public void setCommentCount(int commentCount) {
    this.commentCount = commentCount;
  }

  public int getFavoriteCount() {
    return favoriteCount;
  }

  public void setFavoriteCount(int favoriteCount) {
    this.favoriteCount = favoriteCount;
  }

  @Override
  public String toString() {
    return "StackOverflowPost [id=" + id +  ", title='" + title + "', tags=" + tags + "]";
  }

  
}