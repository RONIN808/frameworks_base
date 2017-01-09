/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "GlLayer.h"

#include "Caches.h"
#include "RenderNode.h"
#include "renderstate/RenderState.h"
#include "utils/TraceUtils.h"

#include <utils/Log.h>

#define ATRACE_LAYER_WORK(label) \
    ATRACE_FORMAT("%s HW Layer DisplayList %s %ux%u", \
            label, \
            (renderNode.get() != NULL) ? renderNode->getName() : "", \
            getWidth(), getHeight())

namespace android {
namespace uirenderer {

GlLayer::GlLayer(RenderState& renderState, uint32_t layerWidth, uint32_t layerHeight)
        : Layer(renderState, Api::OpenGL)
        , caches(Caches::getInstance())
        , texture(caches) {
    texture.mWidth = layerWidth;
    texture.mHeight = layerHeight;
}

GlLayer::~GlLayer() {
    if (texture.mId) {
        texture.deleteTexture();
    }
}

void GlLayer::onGlContextLost() {
    texture.deleteTexture();
}

void GlLayer::bindTexture() const {
    if (texture.mId) {
        caches.textureState().bindTexture(texture.target(), texture.mId);
    }
}

void GlLayer::generateTexture() {
    if (!texture.mId) {
        glGenTextures(1, &texture.mId);
    }
}

void GlLayer::clearTexture() {
    // There's a rare possibility that Caches could have been destroyed already
    // since this method is queued up as a task.
    // Since this is a reset method, treat this as non-fatal.
    if (caches.isInitialized()) {
        caches.textureState().unbindTexture(texture.mId);
    }
    texture.mId = 0;
}

}; // namespace uirenderer
}; // namespace android
