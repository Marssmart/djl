/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.apache.mxnet.nn;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.mxnet.engine.CachedOp;
import org.apache.mxnet.engine.MxModel;
import org.apache.mxnet.engine.MxNDManager;
import org.apache.mxnet.engine.Symbol;
import org.apache.mxnet.jna.JnaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.ai.ndarray.NDList;
import software.amazon.ai.ndarray.NDManager;
import software.amazon.ai.ndarray.types.DataDesc;
import software.amazon.ai.ndarray.types.DataType;
import software.amazon.ai.ndarray.types.Shape;
import software.amazon.ai.nn.AbstractBlock;
import software.amazon.ai.nn.Parameter;
import software.amazon.ai.nn.SymbolBlock;
import software.amazon.ai.training.initializer.Initializer;
import software.amazon.ai.util.PairList;

// TODO: Need to add Memory management for all params
public class MxSymbolBlock extends AbstractBlock implements SymbolBlock {

    private static final Logger logger = LoggerFactory.getLogger(MxModel.class);

    private static final byte VERSION = 1;

    private NDManager manager;
    private CachedOp op;
    private Symbol symbol;
    private List<Parameter> params;
    private boolean paramGradientsAttached;

    public MxSymbolBlock(NDManager manager, Symbol symbol) {
        this(manager, symbol, new ArrayList<>());
    }

    public MxSymbolBlock(NDManager manager, Symbol symbol, List<Parameter> params) {
        this.manager = manager;
        this.symbol = symbol;
        this.params = params;
        initialized = true;
    }

    /**
     * return layers' name.
     *
     * @return an List of String contains layers' nanme
     */
    public List<String> getLayerNames() {
        return symbol.getLayerNames();
    }

    /**
     * Extract the middle layer of SymbolBlock.
     *
     * @param name the layer name. Can be found in {@link MxSymbolBlock#getLayerNames()}
     * @return sliced SymbolBlock
     */
    public SymbolBlock getLayer(String name) {
        Symbol sliced = symbol.get(name);
        HashSet<String> set = new HashSet<>(Arrays.asList(sliced.getAllNames()));
        List<Parameter> slicedParams =
                params.stream()
                        .filter(ele -> set.contains(ele.getName()))
                        .collect(Collectors.toList());
        return new MxSymbolBlock(manager, sliced, slicedParams);
    }

    /**
     * Returns the Symbolic graph from the model.
     *
     * @return {@link Symbol} object
     */
    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public DataDesc[] describeInput() {
        String[] allNames = symbol.getAllNames();
        Map<String, Integer> map = new ConcurrentHashMap<>(allNames.length * 3 / 2);
        List<String> paramNames =
                params.stream().map(Parameter::getName).collect(Collectors.toList());
        int index = 0;
        for (String name : allNames) {
            map.put(name, index++);
        }
        for (String name : paramNames) {
            map.remove(name);
        }
        DataDesc[] inputData = new DataDesc[map.size()];

        index = 0;
        for (String name : map.keySet()) {
            inputData[index++] = new DataDesc(new Shape(), name);
        }
        return inputData;
    }

    @Override
    public void cast(DataType dataType) {
        if (params.get(0).getArray().getDataType() == dataType) {
            logger.debug("You are casting the model to its original type!");
            return;
        }

        // TODO: Not all parameters can be casted.
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public NDList forward(NDList inputs, PairList<String, Object> params) {
        if (!paramGradientsAttached) {
            initializeGradients();
        }
        if (op == null) {
            op = JnaUtils.createCachedOp(this, (MxNDManager) manager);
        }
        return op.forward(inputs);
    }

    public void initializeGradients() {
        for (Parameter param : params) {
            param.getArray().attachGradient();
        }
        paramGradientsAttached = true;
    }

    @Override
    public void backward() {}

    @Override
    public Shape getOutputShape(Shape... inputs) {
        return null;
    }

    @Override
    public List<Parameter> getDirectParameters() {
        return params;
    }

    @Override
    public SymbolBlock removeLastBlock() {
        List<String> layerNames = getLayerNames();
        return getLayer(layerNames.get(layerNames.size() - 2));
    }

    @Override
    public void setInitializer(NDManager manager, Initializer initializer, boolean overwrite) {
        for (Parameter param : params) {
            param.setInitializer(manager, initializer, overwrite);
            param.reinitialize();
        }
    }

    @Override
    public void setInitializer(NDManager manager, Initializer initializer, String paramName) {
        Optional<Parameter> parameter =
                params.stream().filter(pair -> pair.getName().equals(paramName)).findFirst();
        if (parameter.isPresent()) {
            Parameter param = parameter.get();
            param.setInitializer(manager, initializer, false);
            param.reinitialize();
        } else {
            throw new IllegalArgumentException("Could not find parameter " + paramName);
        }
    }

    @Override
    public void beforeInitialize(NDList inputs) {}

    @Override
    public Shape getParameterShape(String name, NDList inputs) {
        for (Parameter param : params) {
            if (param.getName().equals(name)) {
                return param.getArray().getShape();
            }
        }
        throw new IllegalArgumentException("Name " + name + " not found");
    }

    @Override
    public void saveParameters(DataOutputStream os) throws IOException {
        os.writeByte(VERSION);
        for (Parameter parameter : params) {
            parameter.save(os);
        }
    }

    @Override
    public void loadParameters(NDManager manager, DataInputStream is) throws IOException {
        byte version = is.readByte();
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported encoding version: " + version);
        }
        for (Parameter parameter : params) {
            parameter.load(this.manager, is);
        }
    }
}
