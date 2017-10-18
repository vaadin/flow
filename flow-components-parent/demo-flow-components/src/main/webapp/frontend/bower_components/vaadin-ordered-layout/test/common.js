window.positionEquals = (node, top, right, bottom, left) => {
  const rect = node.getBoundingClientRect();
  return rect.top === top && rect.bottom === bottom &&
          rect.left === left && rect.right === right;
};
