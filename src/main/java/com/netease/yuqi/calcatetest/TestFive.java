package com.netease.yuqi.calcatetest;/*
 * Author: park.yq@alibaba-inc.com
 * Date: 2018/9/16 下午5:32
 */

import com.netease.yuqi.aux2.DogVolcanoPlanner;
import com.netease.yuqi.aux2.converter.DogFilterConverter;
import com.netease.yuqi.aux2.converter.DogProjectConverter;
import com.netease.yuqi.aux2.converter.DogTableScanConverter;
import com.netease.yuqi.aux2.cost.DogRelMetaDataProvider;
import com.netease.yuqi.aux2.rel.DogRel;
import com.netease.yuqi.aux2.DogSqlValidator;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.plan.Context;
import org.apache.calcite.plan.ConventionTraitDef;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.plan.RelTraitDef;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.plan.hep.HepPlanner;
import org.apache.calcite.plan.hep.HepProgramBuilder;
import org.apache.calcite.prepare.CalciteCatalogReader;
import org.apache.calcite.rel.RelDistributionTraitDef;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelRoot;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.rel.type.RelDataTypeSystemImpl;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.SqlExplainLevel;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.type.BasicSqlType;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.util.ChainedSqlOperatorTable;
import org.apache.calcite.sql.validate.SqlConformance;
import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.calcite.sql2rel.RelDecorrelator;
import org.apache.calcite.sql2rel.SqlToRelConverter;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.RelBuilder;

import java.util.List;
import java.util.Properties;

public class TestFive {
	public static void main(String[] args) {
		try {
			SchemaPlus rootSchema = Frameworks.createRootSchema(true);
			rootSchema.add("USERS", new AbstractTable() {
				public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
					RelDataTypeFactory.FieldInfoBuilder builder = typeFactory.builder();
					builder.add("ID", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.INTEGER));
					builder.add("NAME", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.CHAR));

					builder.add("TIME_D", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.DATE));
					return builder.build();
				}
			});

			rootSchema.add("SCORE", new AbstractTable() {
				public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
					RelDataTypeFactory.FieldInfoBuilder builder = typeFactory.builder();
					builder.add("ID", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.INTEGER));
					builder.add("SCORE", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.INTEGER));
					return builder.build();
				}
			});


			rootSchema.add("TABLE_RESULT", new AbstractTable() {
				public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
					RelDataTypeFactory.FieldInfoBuilder builder = typeFactory.builder();
					builder.add("ID", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.INTEGER));
					builder.add("NAME", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.CHAR));
					builder.add("SCORE", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.INTEGER));
					return builder.build();
				}
			});


			rootSchema.add("FINAL_RESULT", new AbstractTable() {
				public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
					RelDataTypeFactory.FieldInfoBuilder builder = typeFactory.builder();
					builder.add("ID", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.INTEGER));
					builder.add("SOURCE_DS", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.VARCHAR));
					builder.add("DS", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.VARCHAR));
					return builder.build();
				}
			});

			rootSchema.add("MY_SOURCE", new AbstractTable() {
				public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
					RelDataTypeFactory.FieldInfoBuilder builder = typeFactory.builder();
					builder.add("ID", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.INTEGER));
					builder.add("SOURCE_DS", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.VARCHAR));
					builder.add("DS", new BasicSqlType(new RelDataTypeSystemImpl() {
					}, SqlTypeName.VARCHAR));
					return builder.build();
				}
			});



			final FrameworkConfig config = Frameworks.newConfigBuilder()
					.parserConfig(SqlParser.Config.DEFAULT)
					.defaultSchema(rootSchema)
					.traitDefs(ConventionTraitDef.INSTANCE, RelDistributionTraitDef.INSTANCE)
					.build();
			//String sql = "insert into table_result select id+1, name, score from (select a.id + 1 as id, a.name as name, b.score from users a left join score b on a.id = b.id where a.time_d between '2008-09-12' and cast('2015-09-21' as date) + interval '60' second)";

			String sql = "select id, time_d from users where id between id * 5 and id * id";
			//String sql = "select id, time_d from users";
			//String orSql = "select time_d from users where id > 100 or id > 400 and id in (select id from users where id < 500)";
			//String sql = "insert into final_result select id, cast(1 as varchar) as ds, ds as source_ds from my_source";
			RelNode relNode = sqlToRelNode((SchemaPlus) rootSchema, config, sql);


			HepProgramBuilder builder = new HepProgramBuilder();
			builder.addRuleInstance(DogTableScanConverter.INSTANCE);
			HepPlanner hepPlanner = new HepPlanner(builder.build());
			//hepPlanner.addRule(DogTableScanConverter.INSTANCE);
			hepPlanner.setRoot(relNode);
			relNode = hepPlanner.findBestExp();

			System.out.println("-----------------------------------------------------------");
			System.out.println(RelOptUtil.toString(relNode));
			System.out.println("-----------------------------------------------------------");

			RelOptCluster cluster = relNode.getCluster();
			RelOptPlanner planner = cluster.getPlanner();

			RelTraitSet desiredTraits = cluster.traitSetOf(DogRel.CONVENTION);
			if (!relNode.getTraitSet().equals(desiredTraits)) {
				relNode = cluster.getPlanner().changeTraits(relNode, desiredTraits);
			}

			//todo add some rule to Volcano
			planner.setRoot(relNode);
			RelNode finalNode = planner.findBestExp();

			System.out.println(RelOptUtil.toString(finalNode, SqlExplainLevel.ALL_ATTRIBUTES));
			RelNode tmp = finalNode;
			System.out.println(tmp);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static RelNode sqlToRelNode(SchemaPlus rootScheme, FrameworkConfig frameworkConfig, String sql) {

		try {
			SqlTypeFactoryImpl factory = new SqlTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
			SqlParser parser = SqlParser.create(sql, frameworkConfig.getParserConfig());
			SqlNode sqlNode = parser.parseStmt();


			CalciteCatalogReader calciteCatalogReader =  new CalciteCatalogReader(
					CalciteSchema.from(rootSchema(rootScheme)),
					CalciteSchema.from(frameworkConfig.getDefaultSchema()).path(null),
					factory,
					new CalciteConnectionConfigImpl(new Properties()));


			//to supported user' define function
			SqlOperatorTable sqlOperatorTable = ChainedSqlOperatorTable.of(frameworkConfig.getOperatorTable(), calciteCatalogReader);


			DogSqlValidator validator = new DogSqlValidator(
					sqlOperatorTable,
					calciteCatalogReader,
					factory,
					conformance(frameworkConfig)
			);

			SqlNode validateSqlNode = validator.validate(sqlNode);

			final RexBuilder rexBuilder = createRexBuilder(factory);
			DogVolcanoPlanner volcanoPlanner = new DogVolcanoPlanner();

			volcanoPlanner.clearRelTraitDefs();
			for (RelTraitDef def : frameworkConfig.getTraitDefs()) {
				volcanoPlanner.addRelTraitDef(def);
			}

			final RelOptCluster cluster = RelOptCluster.create(volcanoPlanner, rexBuilder);
			//begin
			cluster.setMetadataProvider(DogRelMetaDataProvider.INSTANCE);
			volcanoPlanner.addRule(DogFilterConverter.INSTANCE);
			volcanoPlanner.addRule(DogProjectConverter.INSTANCE);
			volcanoPlanner.addRule(DogTableScanConverter.INSTANCE);

			//todo add more rule such as filter join/filter project, at this point, rule is
			//light weight

			//volcanoPlanner.addRelTraitDef(ConventionTraitDef.INSTANCE);
			//volcanoPlanner.addRelTraitDef(RelCollationTraitDef.INSTANCE);
			//volcanoPlanner.addRelTraitDef(RelDistributionTraitDef.INSTANCE);
			//end

			final SqlToRelConverter.Config config = SqlToRelConverter.configBuilder()
					.withConfig(frameworkConfig.getSqlToRelConverterConfig())
					.withTrimUnusedFields(false)
					.withConvertTableAccess(false)
					.build();
			final SqlToRelConverter sqlToRelConverter =
					new SqlToRelConverter(new DogView(), validator,
							calciteCatalogReader, cluster, frameworkConfig.getConvertletTable(), config);
			RelRoot root =
					sqlToRelConverter.convertQuery(validateSqlNode, false, true);
			root = root.withRel(sqlToRelConverter.flattenTypes(root.rel, true));
			final RelBuilder relBuilder =
					config.getRelBuilderFactory().create(cluster, null);

//			HepProgramBuilder builder = new HepProgramBuilder();
//			builder.addRuleInstance(DogTableScanConverter.INSTANCE);
//			HepPlanner hepPlanner = new HepPlanner(builder.build());
//			hepPlanner.setRoot(root.rel);
//			root.withRel(hepPlanner.findBestExp());


			root = root.withRel(
					RelDecorrelator.decorrelateQuery(root.rel, relBuilder));

			return root.rel;

		} catch (Exception e) {
			e.printStackTrace();
		}


		return null;
	}

	private static class DogView implements RelOptTable.ViewExpander {
		public DogView() {
		}

		@Override
		public RelRoot expandView(RelDataType rowType, String queryString, List<String> schemaPath, List<String> viewPath) {
			return null;
		}
	}

	private static RexBuilder createRexBuilder(RelDataTypeFactory typeFactory) {
		return new RexBuilder(typeFactory);
	}

	private static SchemaPlus rootSchema(SchemaPlus schema) {
		for (;;) {
			if (schema.getParentSchema() == null) {
				return schema;
			}
			schema = schema.getParentSchema();
		}
	}


	private static SqlConformance conformance(FrameworkConfig config) {
		final Context context = config.getContext();
		if (context != null) {
			final CalciteConnectionConfig connectionConfig =
					context.unwrap(CalciteConnectionConfig.class);
			if (connectionConfig != null) {
				return connectionConfig.conformance();
			}
		}
		return SqlConformanceEnum.DEFAULT;
	}
}

