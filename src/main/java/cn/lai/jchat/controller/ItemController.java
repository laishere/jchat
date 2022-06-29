package cn.lai.jchat.controller;

import javafx.scene.Node;

public interface ItemController<T> {
    Node setItem(T item);
}
